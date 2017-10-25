// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.reactivestreams.{Subscriber, Subscription}
import scala.collection.mutable
import ScalaJsonCacheModule._

class ScalaJsonCache(id: String, backlogLimit: Int, aCache: Cache)
  (implicit system: ActorSystem) extends JsonCache {

  private case class PublishToSubscriber(subscriber: Subscriber[_ >: CacheChangeSet])

  private case class ChangeCache(cacheChangeCalculator: CacheChangeCalculator)

  private case class SendCacheImageToSubscriber(subscriber: Subscriber[_ >: CacheChangeSet])
  
  private case class CompleteAllSubscribers()
  
  private case class FailAllSubscribers(error: Throwable)
  
  private var subscription: Subscription = _
  
  private val cacheActor: ActorRef = system.actorOf(Props(new CacheActor(backlogLimit, aCache)))

  override def getId: String = id

  override def getSubscriberBacklogLimit: Int = backlogLimit

  override def onNext(c: CacheChangeCalculator): Unit = {
    
    requireNotNull(c, "Cannot apply null changes to a JsonCache")
    
    cacheActor ! ChangeCache(c)
    
    val s: Subscription = this.synchronized { subscription }
    
    if(s != null)
      s.request(1)
  }

  override def onSubscribe(s: Subscription): Unit = {
    
    requireNotNull(s, "A JsonCache cannot receive a null Subscription")
    
    this.synchronized { subscription = s }
    
    s.request(1)
  }

  override def onError(error: Throwable): Unit = {
    
    requireNotNull(error, "A JsonCache cannot receive a null error")
    
    cacheActor ! FailAllSubscribers(error)
    
    this.synchronized { subscription = null }
  }

  override def onComplete(): Unit = {
    
    cacheActor ! CompleteAllSubscribers
    
    this.synchronized { subscription = null }
  }

  override def subscribe(s: Subscriber[_ >: CacheChangeSet]): Unit = {
    
    requireNotNull(s, "Cannot subscribe to a JsonCache with a null subscriber")

    cacheActor ! PublishToSubscriber(s)
  }

  override def sendImageToSubscriber(s: Subscriber[_ >: CacheChangeSet]): Unit = {
    
    requireNotNull(s, "Cannot send images of a JsonCache to a null subscriber")

    cacheActor ! SendCacheImageToSubscriber(s)
  }
  
  private class StatefulSubscriber(val delegate: Subscriber[_ >: CacheChangeSet]) extends Subscriber[CacheChangeSet] {

    private var errorOccurred: Boolean = false
    
    override def onError(t: Throwable): Unit = {
      errorOccurred = true
      delegate.onError(t)
    }

    override def onComplete(): Unit = {
      if(!errorOccurred) delegate.onComplete()
    }

    override def onNext(t: CacheChangeSet): Unit = delegate.onNext(t)

    override def onSubscribe(s: Subscription): Unit = delegate.onSubscribe(s)
  }
  
  private class CacheActor(backlogLimit: Int, aCache: Cache) extends Actor {

    private implicit val materializer: ActorMaterializer = ActorMaterializer()(context)

    private var publishers : mutable.Map[ActorRef, StatefulSubscriber]              = mutable.Map()
    private var subscribers: mutable.Map[Subscriber[_ >: CacheChangeSet], ActorRef] = mutable.Map()
    private var cache      : Cache                                                  = aCache

    override def receive: Receive = {

      case ChangeCache(cacheChangeCalculator) =>
        val result: CacheChangeCalculator.ChangeResult = cacheChangeCalculator.calculateChange(cache)
        cache = result.getCache
        publishers.keys.foreach { publisher => publisher ! result.getChangeSet }

      case SendCacheImageToSubscriber(subscriber) => 
        if(subscribers.contains(subscriber)) {
          subscribers(subscriber) ! cache.getImage
        }

      case PublishToSubscriber(subscriber) =>
        val source: Source[CacheChangeSet, ActorRef] = Source.actorRef[CacheChangeSet](backlogLimit, OverflowStrategy.fail)
        val (publisherActor, publisher) = source.toMat(Sink.asPublisher[CacheChangeSet](fanout = false))(Keep.both).run()
        val statefulSubscriber = new StatefulSubscriber(subscriber)
        publishers += (publisherActor -> statefulSubscriber)
        subscribers += (subscriber -> publisherActor)
        context.watch(publisherActor) // Get notified when the publication is cancelled by subscriber: the publisherActor is terminated
        publisherActor ! cache.getImage // Send initial change set
        publisher.subscribe(statefulSubscriber)

      case Terminated(publisherActor) =>
        val subscriber = publishers(publisherActor)
        publishers -= publisherActor
        subscribers -= subscriber.delegate
        subscriber.onComplete() // manual call to onComplete() - otherwise, no indication that publishing has finished

      case FailAllSubscribers(error) =>
        subscribers.keys.foreach { s => s.onError(error) }
        publishers.keys.foreach { p => context.stop(p) }
        context.stop(self)
        publishers.clear()
        subscribers.clear()

      case CompleteAllSubscribers =>
        subscribers.keys.foreach { s => s.onComplete() }
        publishers.keys.foreach { p => context.stop(p) }
        context.stop(self)
        publishers.clear()
        subscribers.clear()
    }
  }
}


