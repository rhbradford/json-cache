// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.modelcoding.opensource.jsoncache.messages.ScalaJsonCacheMessagesModule.requireNotNull
import com.modelcoding.opensource.jsoncache.{CacheChangeSet, CacheMessage}
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.collection.mutable

import scala.collection.JavaConverters._

class ScalaCacheChangeSetOutputStream(implicit system: ActorSystem)
extends CacheChangeSetOutputStream {

  private val setupSync: Object = new Object()

  private var observer: CacheChangeSetOutputStream.Observer = _
  
  override def getCacheChangeSetSubscriber(observer: CacheChangeSetOutputStream.Observer): Subscriber[_ <: CacheChangeSet] = {
   
    requireNotNull(observer, "Cannot use a null observer")
    
    setupSync.synchronized {
     
      if(this.observer != null)
        throw new IllegalStateException("getCacheChangeSetSubscriber can only be called once")

      this.observer = observer
    }
    
    new CacheChangeSetSubscriber()
  }
  
  class CacheChangeSetSubscriber extends Subscriber[CacheChangeSet] {

    private val setupSync: Object = new Object()
  
    override def onError(t: Throwable): Unit = processorActor ! OnInputFailed(t)

    override def onComplete(): Unit = processorActor ! OnInputCompleted()

    override def onNext(msg: CacheChangeSet): Unit = processorActor ! OnInput(msg)

    override def onSubscribe(s: Subscription): Unit = {

      setupSync.synchronized {

        if(processorActor != null)
          throw new IllegalStateException("Subscriber cannot be used twice")
        
        processorActor = system.actorOf(Props(new ProcessorActor))
      }
      
      processorActor ! OnSubscribedToCacheChangeSets(s)
    }
  }
  
  class CacheMessagePublisher extends Publisher[CacheMessage] {

    private val setupSync: Object = new Object()
  
    private var subscribed: Boolean = false
    
    override def subscribe(s: Subscriber[_ >: CacheMessage]): Unit = {

      requireNotNull(s, "Cannot subscribe with a null subscriber")
      
      setupSync.synchronized {
        
        if(subscribed)
          throw new IllegalStateException("Cannot subscribe more than once")
        
        subscribed = true
      }
      
      processorActor ! OnSubscriptionToCacheMessages(s)
    }
  }
  
  private var processorActor: ActorRef = _

  private case class OnSubscribedToCacheChangeSets(s: Subscription)
  private case class OnSubscriptionToCacheMessages(s: Subscriber[_ >: CacheMessage])
  private case class OnCacheMessagesRequested(n: Long)
  private case class OnSubscriptionToCacheMessagesCancelled()
  private case class OnInput(m: CacheChangeSet)
  private case class OnInputFailed(t: Throwable)
  private case class OnInputCompleted()
  private case class OutputCacheMessages()
  
  private class ProcessorActor extends Actor {

    import context._

    private var outputSubscriber : Subscriber[_ >: CacheMessage] = _
    private var inputSubscription: Subscription                  = _

    private val cacheMessages: mutable.Buffer[CacheMessage] = mutable.Buffer() 
    private var cacheMessageDemand: Long = 0

    override def receive: Receive = {

      case OnSubscribedToCacheChangeSets(subscription) =>
        inputSubscription = subscription
        observer.onSubscribed(new CacheMessagePublisher)
        become(subscribedToCacheChangeSets)

      case _ => 
    }
    
    private def subscribedToCacheChangeSets: PartialFunction[Any, Unit] = {
      
      withHandlingOfFinishedStreams({

        case OnSubscriptionToCacheMessages(subscriber) =>
          outputSubscriber = subscriber
          outputSubscriber.onSubscribe(
            new Subscription {
                override def cancel(): Unit = processorActor ! OnSubscriptionToCacheMessagesCancelled()
                override def request(n: Long): Unit = processorActor ! OnCacheMessagesRequested(n)
              }
          )
          become(cacheMessagesSubscriptionStarted)
      })
    }
    
    private def cacheMessagesSubscriptionStarted: PartialFunction[Any, Unit] = {
      
      withHandlingOfFinishedStreams({

        case OnCacheMessagesRequested(numRequested) =>
          cacheMessageDemand += numRequested
          inputSubscription.request(1)
          become(running)

        case OnSubscriptionToCacheMessagesCancelled() =>
          cancel()
      })
    }

    private def running: PartialFunction[Any, Unit] = {
      
      withHandlingOfFinishedStreams({

        case OnCacheMessagesRequested(numRequested) =>
          if(cacheMessages.isEmpty && cacheMessageDemand == 0) {
            inputSubscription.request(1)
          }
          cacheMessageDemand += numRequested
          if(cacheMessages.nonEmpty) {
            self ! OutputCacheMessages()
          }

        case OnInput(cacheChangeSet) =>
          cacheMessages.appendAll(new ScalaCacheChangeSetFrameWrappingChangeSet(cacheChangeSet).getMessages.asScala)
          self ! OutputCacheMessages()

        case OutputCacheMessages() =>
          while(cacheMessageDemand > 0 && cacheMessages.nonEmpty) {
            outputSubscriber.onNext(cacheMessages.remove(0))
            cacheMessageDemand -= 1
          }
          if(cacheMessages.isEmpty && cacheMessageDemand > 0) {
            inputSubscription.request(1)
          }

        case OnSubscriptionToCacheMessagesCancelled() =>
          cancel()
      })
    }

    private def withHandlingOfFinishedStreams(rules: PartialFunction[Any, Unit]): PartialFunction[Any, Unit] =
      rules orElse handleFinishedStreams

    private def handleFinishedStreams: PartialFunction[Any, Unit] = {

      case OnInputFailed(error) => onInputFailed(error)

      case OnInputCompleted() => onInputComplete()
    }

    private def cancel(): Unit = {
      
      outputSubscriber.onComplete()
      inputSubscription.cancel()
    }
    
    private def onInputFailed(error: Throwable): Unit = {

      if(outputSubscriber != null) outputSubscriber.onError(error)
      inputSubscription.cancel()
      context.stop(self)
    }

    private def onInputComplete(): Unit = {

      if(outputSubscriber != null) outputSubscriber.onComplete()
      inputSubscription.cancel()
      context.stop(self)
    }
  }  
}
