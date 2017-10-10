// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import org.reactivestreams.{Publisher, Subscriber}

import scala.collection.mutable

class ScalaJsonCache(id: String, backlogLimit: Int, aCache: Cache)
  (implicit system: ActorSystem, materializer: ActorMaterializer) extends JsonCache {

  private case class PublishToSubscriber(subscriber: Subscriber[_ >: CacheChangeSet])

  private case class ChangeCache(cacheChangeCalculator: CacheChangeCalculator)

  private val cacheActor: ActorRef = system.actorOf(Props(new CacheActor(backlogLimit, aCache)))

  override def getId: String = id

  override def getSubscriberBacklogLimit: Int = backlogLimit

  override def applyChanges(c: CacheChangeCalculator): Unit = cacheActor ! ChangeCache(c)

  override def subscribe(s: Subscriber[_ >: CacheChangeSet]): Unit = cacheActor ! PublishToSubscriber(s)

  class CacheActor(backlogLimit: Int, aCache: Cache) extends Actor {

    private var publishers: mutable.Map[ActorRef, Subscriber[_ >: CacheChangeSet]] = mutable.Map()
    private var cache     : Cache                                                  = aCache

    override def receive: Receive = {

      case ChangeCache(cacheChangeCalculator) =>
        val result: CacheChangeCalculator.ChangeResult = cacheChangeCalculator.calculateChange(cache)
        cache = result.getCache
        publishers.keys.foreach { publisher => publisher ! result.getChangeSet }

      case PublishToSubscriber(subscriber) =>
        val source: Source[CacheChangeSet, ActorRef] = Source.actorRef[CacheChangeSet](backlogLimit, OverflowStrategy.fail)
        val (publisherActor, publisher) = source.toMat(Sink.asPublisher[CacheChangeSet](fanout = false))(Keep.both).run()
        publishers += (publisherActor -> subscriber)
        context.watch(publisherActor) // Get notified when the publication is cancelled by subscriber: the publisherActor is terminated
        publisherActor ! cache.asChangeSet // Send initial change set
        publisher.subscribe(subscriber)

      case Terminated(publisherActor) =>
        val subscriber = publishers(publisherActor)
        publishers -= publisherActor
        subscriber.onComplete() // manual call to onComplete() - otherwise, no indication that publishing has finished
    }
  }
}


