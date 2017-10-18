// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.reactivestreams.{Subscriber, Subscription}

import scala.collection.mutable

class ScalaJsonCache(id: String, backlogLimit: Int, aCache: Cache)
  (implicit system: ActorSystem) extends JsonCache {

  private case class PublishToSubscriber(subscriber: Subscriber[_ >: CacheChangeSet])

  private case class ChangeCache(cacheChangeCalculator: CacheChangeCalculator)

  private case class SendCacheImageToSubscriber(subscriber: Subscriber[_ >: CacheChangeSet])
  
  private val cacheActor: ActorRef = system.actorOf(Props(new CacheActor(backlogLimit, aCache)))

  override def getId: String = id

  override def getSubscriberBacklogLimit: Int = backlogLimit

  override def applyChanges(c: CacheChangeCalculator): Unit = cacheActor ! ChangeCache(c)

  override def subscribe(s: Subscriber[_ >: CacheChangeSet]): Unit = cacheActor ! PublishToSubscriber(s)

  override def sendImageToSubscriber(s: Subscriber[_ >: CacheChangeSet]): Unit = cacheActor ! SendCacheImageToSubscriber(s)
  
  class StatefulSubscriber(val delegate: Subscriber[_ >: CacheChangeSet]) extends Subscriber[CacheChangeSet] {

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
  
  class CacheActor(backlogLimit: Int, aCache: Cache) extends Actor {

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
          subscribers(subscriber) ! cache.asChangeSet()
        }

      case PublishToSubscriber(subscriber) =>
        val source: Source[CacheChangeSet, ActorRef] = Source.actorRef[CacheChangeSet](backlogLimit, OverflowStrategy.fail)
        val (publisherActor, publisher) = source.toMat(Sink.asPublisher[CacheChangeSet](fanout = false))(Keep.both).run()
        val statefulSubscriber = new StatefulSubscriber(subscriber)
        publishers += (publisherActor -> statefulSubscriber)
        subscribers += (subscriber -> publisherActor)
        context.watch(publisherActor) // Get notified when the publication is cancelled by subscriber: the publisherActor is terminated
        publisherActor ! cache.asChangeSet // Send initial change set
        publisher.subscribe(statefulSubscriber)

      case Terminated(publisherActor) =>
        val subscriber = publishers(publisherActor)
        publishers -= publisherActor
        subscribers -= subscriber.delegate
        subscriber.onComplete() // manual call to onComplete() - otherwise, no indication that publishing has finished
    }
  }
}


