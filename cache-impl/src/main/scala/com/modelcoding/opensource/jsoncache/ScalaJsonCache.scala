// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.reactivestreams.{Subscriber, Subscription}

import scala.collection.mutable
import ScalaJsonCacheModule._
import com.modelcoding.opensource.jsoncache.CacheFunction.Result

class ScalaJsonCache(id: String, backlogLimit: Int, aCache: Cache)(implicit system: ActorSystem)
  extends JsonCache {

  private case class RegisterCacheChangeSupplier(subscription: Subscription)
  private case class PublishToSubscriber(subscriber: Subscriber[_ >: CacheChangeSet])
  private case class ChangeCache(cacheChangeCalculator: CacheFunctionInstance)
  private case class SendCacheImageToSubscriber(subscriber: Subscriber[_ >: CacheChangeSet])
  private case class CompleteAllSubscribers()
  private case class FailAllSubscribers(error: Throwable)

  private val cacheActor: ActorRef = system.actorOf(Props(new CacheActor(backlogLimit, aCache)))

  override def getId: String = id

  override def getSubscriberBacklogLimit: Int = backlogLimit

  override def onNext(c: CacheFunctionInstance): Unit = {

    requireNotNull(c, "Cannot apply null changes to a JsonCache")

    cacheActor ! ChangeCache(c)
  }

  override def onSubscribe(s: Subscription): Unit = {

    requireNotNull(s, "A JsonCache cannot receive a null Subscription")

    cacheActor ! RegisterCacheChangeSupplier(s)
  }

  override def onError(error: Throwable): Unit = {

    requireNotNull(error, "A JsonCache cannot receive a null error")

    cacheActor ! FailAllSubscribers(error)
  }

  override def onComplete(): Unit = {

    cacheActor ! CompleteAllSubscribers
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

    private val publishers         : mutable.Map[ActorRef, StatefulSubscriber]              = mutable.Map()
    private val subscribers        : mutable.Map[Subscriber[_ >: CacheChangeSet], ActorRef] = mutable.Map()
    private var cache              : Cache                                                  = aCache
    private var cacheChangeSupplier: Subscription                                           = _

    override def receive: Receive = {

      case ChangeCache(cacheFunctionInstance) =>
        val result: Result = cacheFunctionInstance.getCode.execute(cache)
        if(cache ne result.getCache) {
          cache = result.getCache
          publishers.keys.foreach { publisher => publisher ! result.getChangeSet }
        }
        if(cacheChangeSupplier != null) cacheChangeSupplier.request(1)

      case SendCacheImageToSubscriber(subscriber) =>
        if(subscribers.contains(subscriber)) {subscribers(subscriber) ! cache.getImage}

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
        subscriber.onComplete() // explicit call to onComplete() - otherwise, no indication that publishing has finished

      case RegisterCacheChangeSupplier(s) =>
        cacheChangeSupplier = s
        cacheChangeSupplier.request(1)

      case FailAllSubscribers(error) =>
        publishers.values.foreach { s => s.onError(error) }
        stopAndClearUp()

      case CompleteAllSubscribers =>
        publishers.values.foreach { s => s.onComplete() }
        stopAndClearUp()
    }

    private def stopAndClearUp(): Unit = {

      publishers.keys.foreach { p => context.stop(p) }
      context.stop(self)
      publishers.clear()
      subscribers.clear()
      cacheChangeSupplier = null
    }
  }
}


