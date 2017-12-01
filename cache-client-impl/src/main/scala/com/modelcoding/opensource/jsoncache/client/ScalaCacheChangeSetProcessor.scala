// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import java.util.function.Predicate

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.modelcoding.opensource.jsoncache._
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.collection.JavaConverters._

import scala.collection.mutable
import ScalaJsonCacheClientModule._

class ScalaCacheChangeSetProcessor(
  cacheObjectSelectors: Publisher[Predicate[CacheObject]]
)
  (implicit jsonCacheModule: JsonCacheModule, system: ActorSystem)
  extends CacheChangeSetProcessor {

  private var input     : CacheImageSender                = _
  private var subscriber: Subscriber[_ >: CacheChangeSet] = _

  override def connect(
    input: CacheImageSender
  ): Unit = {

    requireNotNull(input, "A CacheChangeSetProcessor cannot be connected with a null input")
    
    this.synchronized {
     
      if(this.input != null)
        throw new IllegalStateException("A CacheChangeSetProcessor cannot be connected more than once")

      this.input = input
    }
  }

  override def subscribe(
    subscriber: Subscriber[_ >: CacheChangeSet]
  ): Unit = {

    requireNotNull(subscriber, "Cannot subscribe to a CacheChangeSetProcessor with a null subscriber")
    
    this.synchronized {
      
      if(input == null)
        throw new IllegalStateException("Cannot subscribe to an unconnected CacheChangeSetProcessor - call connect first")
      if(this.subscriber != null)
        throw new IllegalStateException("Cannot subscribe more than once to a CacheChangeSetProcessor")

      this.subscriber = subscriber
    }

    processorActor = system.actorOf(Props(new ProcessorActor()))

    processorActor ! SubscribeToSelectors
  }

  override def sendImageToSubscriber(
    subscriber: Subscriber[_ >: CacheChangeSet]
  ): Unit = {
    
    requireNotNull(subscriber, "A CacheChangeSetProcessor cannot send a cache image to a null subscriber")
    
    this.synchronized {
      
      if(input == null)
        throw new IllegalStateException("An unconnected CacheChangeSetProcessor cannot send a cache image - call connect first")
      if(this.subscriber == null)
        throw new IllegalStateException("An unsubscribed CacheChangeSetProcessor cannot send a cache image - call subscribe first")
      require(this.subscriber == subscriber, "A CacheChangeSetProcessor can only send images to the subscriber given in subscribe")
    }
    
    processorActor ! SendCacheImageToSubscriber
  }

  private var processorActor: ActorRef = _

  private case class SubscribeToSelectors()
  private case class OnSelectorsSubscribed()
  private case class OnNextSelector(selector: Predicate[CacheObject])
  private case class OnSelectorsFailed(t: Throwable)
  private case class OnSelectorsCompleted()
  private case class OnInputSubscribed()
  private case class OnNextChangeSetFromInput(changeSet: CacheChangeSet)
  private case class OnInputFailed(t: Throwable)
  private case class OnInputCompleted()
  private case class CancelOutput()
  private case class RequestOutputChangeSets(n: Long)
  private case class SendCacheImageToSubscriber()

  private class ProcessorActor extends Actor {

    import context._

    private var selectorSubscriber   : Subscriber[Predicate[CacheObject]] = _
    private var selectorsSubscription: Subscription                       = _
    private var inputSubscriber      : Subscriber[CacheChangeSet]         = _
    private var inputSubscription    : Subscription                       = _
    private var selector             : Predicate[CacheObject]             = _
    private var pendingSelector      : Predicate[CacheObject]             = _

    override def receive: Receive = {

      case SubscribeToSelectors =>
        subscribeToSelectors()
        become(subscribingToSelectors)

      case SendCacheImageToSubscriber =>  
    }

    private def subscribingToSelectors: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case OnSelectorsSubscribed =>
          selectorsSubscription.request(1)
          become(subscribedToSelectors)

        case SendCacheImageToSubscriber =>  
      })
    }
    
    private def subscribedToSelectors: PartialFunction[Any, Unit] = {
      
      withHandlingOfFinishedStreams({

        case OnNextSelector(s) =>
          selector = s
          selectorsSubscription.request(1)
          subscribeToInput()
          become(subscribingToInput)

        case SendCacheImageToSubscriber =>  
      })
    }

    private def subscribingToInput: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case OnInputSubscribed =>
          subscriber.onSubscribe(new OutputSubscription)
          become(running)

        case OnNextSelector(s) =>
          selector = s
          selectorsSubscription.request(1)

        case SendCacheImageToSubscriber =>  
      })
    }

    private def running: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case CancelOutput => onCancelOutput()
        
        case RequestOutputChangeSets(n) =>
          inputSubscription.request(n)

        case OnNextChangeSetFromInput(changeSet) =>
          processChangeSet(changeSet)

        case OnNextSelector(s) =>
          pendingSelector = s
          selectorsSubscription.request(1)
          input.sendImageToSubscriber(inputSubscriber)
          become(runningPendingSelectorChange)

        case SendCacheImageToSubscriber =>
          input.sendImageToSubscriber(inputSubscriber)
      })
    }

    private def runningPendingSelectorChange: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case CancelOutput => onCancelOutput()
        
        case RequestOutputChangeSets(n) =>
          inputSubscription.request(n)

        case OnNextChangeSetFromInput(changeSet) =>
          if(changeSet.isCacheImage) {
            selector = pendingSelector
            pendingSelector = null
            become(running)
          }
          processChangeSet(changeSet)
          inputSubscription.request(1)

        case OnNextSelector(s) =>
          pendingSelector = s
          selectorsSubscription.request(1)
          input.sendImageToSubscriber(inputSubscriber)

        case SendCacheImageToSubscriber =>
          input.sendImageToSubscriber(inputSubscriber)
      })
    }

    private def withHandlingOfFinishedStreams(rules: PartialFunction[Any, Unit]): PartialFunction[Any, Unit] =
      rules orElse handleFinishedStreams

    private def handleFinishedStreams: PartialFunction[Any, Unit] = {

      case OnInputFailed(error) => onInputFailed(error)

      case OnInputCompleted => onInputComplete()

      case OnSelectorsFailed(error) => onSelectorsFailed(error)

      case OnSelectorsCompleted => onSelectorsComplete()
    }

    private def onCancelOutput(): Unit = {
      
      selectorsSubscription.cancel()
      inputSubscription.cancel()
      subscriber.onComplete()
      context.stop(self)
    }
    
    private def subscribeToInput(): Unit = {

      inputSubscriber = new InputSubscriber
      input.subscribe(inputSubscriber)
    }

    private def processChangeSet(changeSet: CacheChangeSet): Unit = {

      val removes: mutable.Set[CacheRemove] = new mutable.HashSet[CacheRemove]()
      removes ++= changeSet.getRemoves.asScala

      val puts: mutable.Set[CacheObject] = new mutable.HashSet[CacheObject]()
      changeSet.getPuts.asScala.foreach { put: CacheObject =>

        if(selector != null && selector.test(put))  
          puts += put
        else
          removes += put.asCacheRemove()
      }

      val outputChangeSet: CacheChangeSet = 
        jsonCacheModule.getCacheChangeSet(changeSet.getId, puts.asJava, removes.asJava, changeSet.isCacheImage)

      subscriber.onNext(outputChangeSet)
    }

    private def onInputFailed(error: Throwable): Unit = {

      subscriber.onError(error)
      selectorsSubscription.cancel()
      context.stop(self)
    }

    private def onInputComplete(): Unit = {

      subscriber.onComplete()
      selectorsSubscription.cancel()
      context.stop(self)
    }

    private def subscribeToSelectors(): Unit = {

      selectorSubscriber = new SelectorSubscriber
      cacheObjectSelectors.subscribe(selectorSubscriber)
    }

    private def onSelectorsFailed(error: Throwable): Unit = {

      subscriber.onError(error)
      if(inputSubscription != null) inputSubscription.cancel()
      context.stop(self)
    }

    private def onSelectorsComplete(): Unit = {

      if(selector == null) {
        // Finished - not going to get a selector to use to process the change sets
        subscriber.onComplete()
        context.stop(self)
      } // otherwise - continue, as there is a selector (note that this selector will not now change...)
    }

    private class OutputSubscription() extends Subscription {

      override def cancel(): Unit = processorActor ! CancelOutput

      override def request(n: Long): Unit = processorActor ! RequestOutputChangeSets(n)
    }
    
    private class InputSubscriber extends Subscriber[CacheChangeSet] {

      override def onError(t: Throwable): Unit = processorActor ! OnInputFailed(t)

      override def onComplete(): Unit = processorActor ! OnInputCompleted

      override def onNext(t: CacheChangeSet): Unit = {

        processorActor ! OnNextChangeSetFromInput(t)
      }

      override def onSubscribe(s: Subscription): Unit = {

        inputSubscription = s
        processorActor ! OnInputSubscribed
      }
    }

    private class SelectorSubscriber extends Subscriber[Predicate[CacheObject]] {

      override def onError(t: Throwable): Unit = processorActor ! OnSelectorsFailed(t)

      override def onComplete(): Unit = processorActor ! OnSelectorsCompleted

      override def onNext(t: Predicate[CacheObject]): Unit = {

        processorActor ! OnNextSelector(t)
        selectorsSubscription.request(1)
      }

      override def onSubscribe(s: Subscription): Unit = {

        selectorsSubscription = s
        processorActor ! OnSelectorsSubscribed
      }
    }
  }
}
