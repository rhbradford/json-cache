// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.modelcoding.opensource.jsoncache.messages.ScalaJsonCacheMessagesModule.requireNotNull
import com.modelcoding.opensource.jsoncache.{CacheChangeSet, CacheMessage}
import org.reactivestreams.{Publisher, Subscriber, Subscription}

class ScalaCacheChangeSetInputStream(val assembler: CacheChangeSetFrameAssembler)
  (implicit system: ActorSystem)
extends CacheChangeSetInputStream {

  private val setupSync: Object = new Object()

  private var observer: CacheChangeSetInputStream.Observer = _
  
  override def getCacheMessageSubscriber(observer: CacheChangeSetInputStream.Observer): Subscriber[_ <: CacheMessage] = {
   
    requireNotNull(observer, "Cannot use a null observer")
    
    setupSync.synchronized {
     
      if(this.observer != null)
        throw new IllegalStateException("getCacheMessageSubscriber can only be called once")

      this.observer = observer
    }
    
    new CacheMessageSubscriber()
  }
  
  class CacheMessageSubscriber extends Subscriber[CacheMessage] {

    private val setupSync: Object = new Object()
  
    override def onError(t: Throwable): Unit = processorActor ! OnInputFailed(t)

    override def onComplete(): Unit = processorActor ! OnInputCompleted()

    override def onNext(msg: CacheMessage): Unit = processorActor ! OnInput(msg)

    override def onSubscribe(s: Subscription): Unit = {

      setupSync.synchronized {

        if(processorActor != null)
          throw new IllegalStateException("Subscriber cannot be used twice")
        
        processorActor = system.actorOf(Props(new ProcessorActor))
      }
      
      processorActor ! OnSubscribedToCacheMessages(s)
    }
  }
  
  class CacheChangeSetPublisher extends Publisher[CacheChangeSet] {

    private val setupSync: Object = new Object()
  
    private var subscribed: Boolean = false
    
    override def subscribe(s: Subscriber[_ >: CacheChangeSet]): Unit = {

      requireNotNull(s, "Cannot subscribe with a null subscriber")
      
      setupSync.synchronized {
        
        if(subscribed)
          throw new IllegalStateException("Cannot subscribe more than once")
        
        subscribed = true
      }
      
      processorActor ! OnSubscriptionToCacheChangeSets(s)
    }
  }
  
  private var processorActor: ActorRef = _

  private case class OnSubscribedToCacheMessages(s: Subscription)
  private case class OnSubscriptionToCacheChangeSets(s: Subscriber[_ >: CacheChangeSet])
  private case class OnCacheChangeSetsRequested(n: Long)
  private case class OnSubscriptionToCacheChangeSetsCancelled()
  private case class OnInput(m: CacheMessage)
  private case class OnInputFailed(t: Throwable)
  private case class OnInputCompleted()
  
  private class ProcessorActor extends Actor {

    import context._

    private var outputSubscriber : Subscriber[_ >: CacheChangeSet] = _
    private var inputSubscription: Subscription                    = _

    private var cacheChangeSetDemand: Long = 0

    override def receive: Receive = {

      case OnSubscribedToCacheMessages(subscription) =>
        inputSubscription = subscription
        observer.onSubscribed(new CacheChangeSetPublisher)
        become(subscribedToCacheMessages)

      case _ => 
    }
    
    private def subscribedToCacheMessages: PartialFunction[Any, Unit] = {
      
      withHandlingOfFinishedStreams({

        case OnSubscriptionToCacheChangeSets(subscriber) =>
          outputSubscriber = subscriber
          outputSubscriber.onSubscribe(
            new Subscription {
                override def cancel(): Unit = processorActor ! OnSubscriptionToCacheChangeSetsCancelled()
                override def request(n: Long): Unit = processorActor ! OnCacheChangeSetsRequested(n)
              }
          )
          become(cacheChangeSetsSubscriptionStarted)
      })
    }
    
    private def cacheChangeSetsSubscriptionStarted: PartialFunction[Any, Unit] = {
      
      withHandlingOfFinishedStreams({

        case OnCacheChangeSetsRequested(numRequested) =>
          assembler.connect((cacheChangeSetFrame: CacheChangeSetFrame) => {
            outputSubscriber.onNext(cacheChangeSetFrame.getCacheChangeSet)
            cacheChangeSetDemand -= 1
          })
          cacheChangeSetDemand += numRequested
          inputSubscription.request(1)
          become(running)

        case OnSubscriptionToCacheChangeSetsCancelled() =>
          cancel()
      })
    }

    private def running: PartialFunction[Any, Unit] = {
      
      withHandlingOfFinishedStreams({

        case OnCacheChangeSetsRequested(numRequested) =>
          if(cacheChangeSetDemand == 0)
            inputSubscription.request(1)
          cacheChangeSetDemand += numRequested

        case OnInput(cacheMessage) =>
          try {
            assembler.onCacheMessage(cacheMessage)
            if(cacheChangeSetDemand > 0) inputSubscription.request(1)
          }
          catch {
            case error: Throwable => 
              onInputFailed(error)
          }

        case OnSubscriptionToCacheChangeSetsCancelled() =>
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
