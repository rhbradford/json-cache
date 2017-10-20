// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import java.util.function.Predicate

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.modelcoding.opensource.jsoncache._
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.collection.JavaConverters._

import scala.collection.mutable

class ScalaCacheChangeSetProcessor(cacheObjectSelectors: Publisher[Predicate[CacheObject]])
  (implicit jsonCacheModule: JsonCacheModule, system: ActorSystem)
  extends CacheChangeSetProcessor {

  private var jsonCache : JsonCache                       = _
  private var input     : Publisher[CacheChangeSet]       = _
  private var subscriber: Subscriber[_ >: CacheChangeSet] = _

  override def connect(
    jsonCache: JsonCache,
    input: Publisher[CacheChangeSet]
  ): Unit = {

    require(jsonCache != null, "A CacheChangeSetProcessor cannot be connected with a null jsonCache")
    require(input != null, "A CacheChangeSetProcessor cannot be connected with a null input")

    this.jsonCache = jsonCache
    this.input = input
  }

  override def subscribe(
    subscriber: Subscriber[_ >: CacheChangeSet]
  ): Unit = {

    require(subscriber!= null, "Cannot subscribe to a CacheChangeSetProcessor with a null subscriber")
    require(input != null, "Cannot subscribe to an unconnected CacheChangeSetProcessor - call connect(JsonCache,Publisher) first")
    require(processorActor == null, "Cannot subscribe more then once to a CacheChangeSetProcessor")

    this.subscriber = subscriber

    processorActor = system.actorOf(Props(new ProcessorActor()))

    processorActor ! SubscribeToSelectors
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

  private class ProcessorActor extends Actor {

    import context._

    private var selectorsSubscription : Subscription           = _
    private var changeSetsSubscription: Subscription           = _
    private var selector              : Predicate[CacheObject] = _
    private var pendingSelector       : Predicate[CacheObject] = _

    override def receive: Receive = {

      case SubscribeToSelectors =>
        subscribeToSelectors()
        become(subscribingToSelectors)
    }

    private def subscribingToSelectors: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case OnSelectorsSubscribed =>
          selectorsSubscription.request(1)
          subscribeToChangeSets()
          become(subscribingToChangeSets)
      })
    }

    private def subscribingToChangeSets: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case OnChangeSetsSubscribed =>
          become(subscribedToChangeSetsWithNoSelector)

        case OnNextSelector(s) =>
          selector = s
          selectorsSubscription.request(1)
          become(subscribingToChangeSetsWithSelector)
      })
    }

    private def subscribedToChangeSetsWithNoSelector: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({

        case OnNextSelector(s) =>
          selector = s
          selectorsSubscription.request(1)
          changeSetsSubscription.request(1)
          become(running)
      })
    }

    private def subscribingToChangeSetsWithSelector: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({
        
        case OnChangeSetsSubscribed =>
          changeSetsSubscription.request(1)
          become(running)

        case OnNextSelector(s) =>
          selector = s
          selectorsSubscription.request(1)
      })
    }

    private def running: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({
        
        case OnNextChangeSet(changeSet) => 
          processChangeSet(changeSet)
          changeSetsSubscription.request(1)

        case OnNextSelector(s) =>
          pendingSelector = s
          selectorsSubscription.request(1)
          become(runningPendingSelectorChange)
      })
    }

    private def runningPendingSelectorChange: PartialFunction[Any, Unit] = {

      withHandlingOfFinishedStreams({
        
        case OnNextChangeSet(changeSet) =>
          if(changeSet.isInstanceOf[CacheImage]) {
            selector = pendingSelector
            pendingSelector = _
            become(running)
          }
          processChangeSet(changeSet)
          changeSetsSubscription.request(1)

        case OnNextSelector(s) =>
          pendingSelector = s
          selectorsSubscription.request(1)
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

      input.subscribe(new Subscriber[CacheChangeSet] {

        override def onError(t: Throwable): Unit = processorActor ! OnChangeSetsFailed(t)

        override def onComplete(): Unit = processorActor ! OnChangeSetsCompleted

        override def onNext(t: CacheChangeSet): Unit = {

          processorActor ! OnNextChangeSet(t)
          changeSetsSubscription.request(1)
        }

        override def onSubscribe(s: Subscription): Unit = {

          changeSetsSubscription = s
          processorActor ! OnChangeSetsSubscribed
        }
      })
    }

    private def processChangeSet(changeSet: CacheChangeSet): Unit = {

      val removes: mutable.Set[CacheRemove] = new mutable.HashSet[CacheRemove]()
      removes ++ changeSet.getRemoves.asScala

      val puts: mutable.Set[CacheObject] = new mutable.HashSet[CacheObject]()
      changeSet.getPuts.asScala.foreach { put: CacheObject =>

        if(selector.test(put)) puts + put
      }

      val outputChangeSet: CacheChangeSet =
        if(changeSet.isInstanceOf[CacheImage])
          jsonCacheModule.getCacheImage(puts.asJava)
        else
          jsonCacheModule.getCacheChangeSet(puts.asJava, removes.asJava)

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

      cacheObjectSelectors.subscribe(new Subscriber[Predicate[CacheObject]] {

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
      })
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
  }
}
