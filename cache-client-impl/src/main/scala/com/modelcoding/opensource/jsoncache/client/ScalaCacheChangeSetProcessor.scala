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
    if(this.input != null)
      throw new IllegalStateException("A CacheChangeSetProcessor cannot be connected more than once")
    
    this.input = input
  }

  override def subscribe(
    subscriber: Subscriber[_ >: CacheChangeSet]
  ): Unit = {

    requireNotNull(subscriber, "Cannot subscribe to a CacheChangeSetProcessor with a null subscriber")
    if(input == null)
      throw new IllegalStateException("Cannot subscribe to an unconnected CacheChangeSetProcessor - call connect first")
    if(this.subscriber != null)
      throw new IllegalStateException("Cannot subscribe more then once to a CacheChangeSetProcessor")

    this.subscriber = subscriber

    processorActor = system.actorOf(Props(new ProcessorActor()))

    processorActor ! SubscribeToSelectors
  }

  override def sendImageToSubscriber(
    subscriber: Subscriber[_ >: CacheChangeSet]
  ): Unit = {
    
    requireNotNull(subscriber, "A CacheChangeSetProcessor cannot send a cache image to a null subscriber")
    if(input == null)
      throw new IllegalStateException("An unconnected CacheChangeSetProcessor cannot send a cache image - call connect first")
    if(this.subscriber == null)
      throw new IllegalStateException("An unsubscribed CacheChangeSetProcessor cannot send a cache image - call subscribe first")
    require(this.subscriber == subscriber, "A CacheChangeSetProcessor can only send images to the subscriber given in subscribe")
    
    processorActor ! SendCacheImageToSubscriber
  }

  private var processorActor: ActorRef = _

  private case class SubscribeToSelectors()
  private case class OnSelectorsSubscribed()
  private case class OnNextSelector(selector: Predicate[CacheObject])
  private case class OnSelectorsFailed(t: Throwable)
  private case class OnSelectorsCompleted()
  private case class OnChangeSetsSubscribed()
  private case class OnNextChangeSet(changeSet: CacheChangeSet)
  private case class OnChangeSetsFailed(t: Throwable)
  private case class OnChangeSetsCompleted()
  private case class CancelChangeSets()
  private case class RequestChangeSets(n: Long)
  private case class SendCacheImageToSubscriber()

  private class ProcessorActor extends Actor {

    import context._

    private var selectorSubscriber    : Subscriber[Predicate[CacheObject]] = _
    private var selectorsSubscription : Subscription                       = _
    private var changeSetSubscriber   : Subscriber[CacheChangeSet]         = _
    private var changeSetsSubscription: Subscription                       = _
    private var selector              : Predicate[CacheObject]             = _
    private var pendingSelector       : Predicate[CacheObject]             = _

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
          subscribeToChangeSets()
          become(subscribingToChangeSets)

        case SendCacheImageToSubscriber =>  
      })
    }

    private def subscribingToChangeSets: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case OnChangeSetsSubscribed =>
          subscriber.onSubscribe(new ChangeSetSubscription)
          become(running)

        case OnNextSelector(s) =>
          selector = s
          selectorsSubscription.request(1)

        case SendCacheImageToSubscriber =>  
      })
    }

    private def running: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case CancelChangeSets => changeSetsSubscription.cancel()
        
        case RequestChangeSets(n) =>
          changeSetsSubscription.request(n)

        case OnNextChangeSet(changeSet) =>
          processChangeSet(changeSet)

        case OnNextSelector(s) =>
          pendingSelector = s
          selectorsSubscription.request(1)
          input.sendImageToSubscriber(changeSetSubscriber)
          become(runningPendingSelectorChange)

        case SendCacheImageToSubscriber =>
          input.sendImageToSubscriber(changeSetSubscriber)
      })
    }

    private def runningPendingSelectorChange: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case CancelChangeSets => changeSetsSubscription.cancel()
        
        case RequestChangeSets(n) =>
          changeSetsSubscription.request(n)

        case OnNextChangeSet(changeSet) =>
          if(changeSet.isCacheImage) {
            selector = pendingSelector
            pendingSelector = null
            become(running)
          }
          processChangeSet(changeSet)
          changeSetsSubscription.request(1)

        case OnNextSelector(s) =>
          pendingSelector = s
          selectorsSubscription.request(1)

        case SendCacheImageToSubscriber =>
          input.sendImageToSubscriber(changeSetSubscriber)
      })
    }

    private def withHandlingOfFinishedStreams(rules: PartialFunction[Any, Unit]): PartialFunction[Any, Unit] =
      rules orElse handleFinishedStreams

    private def handleFinishedStreams: PartialFunction[Any, Unit] = {

      case OnChangeSetsFailed(error) => onChangeSetsFailed(error)

      case OnChangeSetsCompleted => onChangeSetsComplete()

      case OnSelectorsFailed(error) => onSelectorsFailed(error)

      case OnSelectorsCompleted => onSelectorsComplete()
    }

    private def subscribeToChangeSets(): Unit = {

      changeSetSubscriber = new ChangeSetSubscriber
      input.subscribe(changeSetSubscriber)
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
        jsonCacheModule.getCacheChangeSet(puts.asJava, removes.asJava, changeSet.isCacheImage)

      subscriber.onNext(outputChangeSet)
    }

    private def onChangeSetsFailed(error: Throwable): Unit = {

      subscriber.onError(error)
      selectorsSubscription.cancel()
      context.stop(self)
    }

    private def onChangeSetsComplete(): Unit = {

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
      if(changeSetsSubscription != null) changeSetsSubscription.cancel()
      context.stop(self)
    }

    private def onSelectorsComplete(): Unit = {

      if(selector == null) {
        // Finished - not going to get a selector to use to process the change sets
        subscriber.onComplete()
        if(changeSetsSubscription != null) changeSetsSubscription.cancel()
        context.stop(self)
      } // otherwise - continue, as there is a selector (note that this selector will not now change...)
    }

    private class ChangeSetSubscription() extends Subscription {

      override def cancel(): Unit = processorActor ! CancelChangeSets

      override def request(n: Long): Unit = processorActor ! RequestChangeSets(n)
    }
    
    private class ChangeSetSubscriber extends Subscriber[CacheChangeSet] {

      override def onError(t: Throwable): Unit = processorActor ! OnChangeSetsFailed(t)

      override def onComplete(): Unit = processorActor ! OnChangeSetsCompleted

      override def onNext(t: CacheChangeSet): Unit = {

        processorActor ! OnNextChangeSet(t)
      }

      override def onSubscribe(s: Subscription): Unit = {

        changeSetsSubscription = s
        processorActor ! OnChangeSetsSubscribed
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
