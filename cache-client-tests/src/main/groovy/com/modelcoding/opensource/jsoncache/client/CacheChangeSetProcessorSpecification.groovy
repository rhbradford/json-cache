// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import com.modelcoding.opensource.jsoncache.CacheImageSender
import com.modelcoding.opensource.jsoncache.CacheObject
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.IgnoreRest
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Predicate

import static TestSuite.*

class CacheChangeSetProcessorSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup
    
    def "CacheChangeSetProcessor is created as expected"() {
        
        when:
        def selectors = Mock(Publisher)
        c.getCacheChangeSetProcessor(selectors)
        
        then:
        notThrown(Throwable)
        0 * selectors._
    }   
    
    def "CacheChangeSetProcessor cannot be created from bad parameters"() {
        
        when:
        c.getCacheChangeSetProcessor(null)
        
        then:
        thrown(NullPointerException)
    }
    
    def "CacheChangeSetProcessor cannot be connected with bad parameters"() {
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(null)
        
        then:
        thrown(NullPointerException)
    }
    
    def "CacheChangeSetProcessor cannot be subscribed to with bad parameters"() {
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(Mock(CacheImageSender))
        processor.subscribe(null)
        
        then:
        thrown(NullPointerException)
    }
    
    def "CacheChangeSetProcessor cannot be requested to send cache image with bad parameters"() {
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(Mock(CacheImageSender))
        processor.subscribe(Mock(Subscriber))
        processor.sendImageToSubscriber(null)
        
        then:
        thrown(NullPointerException)
    }
    
    def "CacheChangeSetProcessor cannot be subscribed to unless connected"() {
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.subscribe(Mock(Subscriber))
        
        then:
        thrown(IllegalStateException)
    }
    
    def "CacheChangeSetProcessor cannot be requested to send cache image unless connected and subscribed"() {
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.sendImageToSubscriber(Mock(Subscriber))
        
        then:
        thrown(IllegalStateException)
        
        when:
        processor.connect(Mock(CacheImageSender))
        processor.sendImageToSubscriber(Mock(Subscriber))
        
        then:
        thrown(IllegalStateException)
    }
    
    def "CacheChangeSetProcessor can only send cache images to its subscriber"() {
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        def subscriber = Mock(Subscriber)
        processor.connect(Mock(CacheImageSender))
        processor.subscribe(subscriber)
        processor.sendImageToSubscriber(Mock(Subscriber))
        
        then:
        thrown(IllegalArgumentException)
    }
    
    def "CacheChangeSetProcessor cannot be subscribed to twice"() {
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(Mock(CacheImageSender))
        processor.subscribe(Mock(Subscriber))
        processor.subscribe(Mock(Subscriber))
        
        then:
        thrown(IllegalStateException)
    }
    
    def "CacheChangeSetProcessor does not interact with its input on connect"() {
        
        setup:
        def input = Mock(CacheImageSender)
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        
        then:
        0 * input._
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetProcessor subscribes to the selectors when it is subscribed to"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = Mock(CacheImageSender)
        def subscriber = Mock(Subscriber)
        
        when:
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then:
        selectors.awaitSubscription()
        0 * input.subscribe(_)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetProcessor only subscribes to its input when the first selector is received"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = Mock(Subscriber)
        def aSelector = { cacheObject -> true } as Predicate<CacheObject>
        def selectorsSubscription = Mock(Subscription) {
            1 * request(1) >> { 
                selectors.subscriber.onNext(aSelector) 
            }
            _ * request(1)
        }
        
        when:
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then:
        selectors.awaitSubscription()
        input.subscriber == null
        
        when:
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then:
        input.awaitSubscription()
        input.subscriber != null
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetProcessor completes its subscriber if selectors subscription completes without publishing any selectors"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = Mock(Subscription) {
            1 * request(1) >> { 
                selectors.subscriber.onComplete()
            }
            _ * request(1)
        }
        
        when:
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then:
        selectors.awaitSubscription()
        
        when:
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then:
        subscriber.awaitCompleted()
    }

    def "CacheChangeSetProcessor manages received selectors and filters change sets as expected"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        then: "on receipt of the selector, the CacheChangeSetProcessor subscribes to its input"
        input.awaitSubscription()
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the CacheChangeSetProcessor establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([])),
                        m.getCacheObject("B1", "BType", asJsonNode([])),
                        m.getCacheObject("C1", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    true
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A3", "AType", asJsonNode([])),
                        m.getCacheObject("B2", "BType", asJsonNode([])),
                        m.getCacheObject("C2", "CType", asJsonNode([]))
                    ] as Set,
                    [
                        m.getCacheRemove("A2")
                    ] as Set,
                    false
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A3", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }
        
        when: "the CacheChangeSetProcessor receives another selector"
        input.expectSendImageRequest()
        def selectorRequested = selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor requests a cache image from its input, and makes the selector the pending selector"
        input.awaitSendImageRequest()
        input.sendImageSubscriber == input.subscriber
        selectorRequested
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A4", "AType", asJsonNode([])),
                        m.getCacheObject("B3", "BType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    false
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using current selector (not the pending selector), and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A4", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("B3")
                ] as Set,
                false
            )
        }
        
        when: "the CacheChangeSetProcessor receives another selector"
        input.expectSendImageRequest()
        selectorRequested = selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("C") } as Predicate<CacheObject>)
        }
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor requests a cache image from its input, and makes this selector replace the pending selector as the new pending selector"
        input.awaitSendImageRequest()
        input.sendImageSubscriber == input.subscriber
        selectorRequested
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("C3", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    false
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using current selector (not the pending selector), and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [] as Set,
                [
                    m.getCacheRemove("C3")
                ] as Set,
                false
            )
        }
        
        when: "the input sends a cache image"
        input.sendImageSubscriber.onNext(
            m.getCacheChangeSet(
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A3", "AType", asJsonNode([])),
                    m.getCacheObject("A4", "AType", asJsonNode([])),
                    m.getCacheObject("B1", "BType", asJsonNode([])),
                    m.getCacheObject("B2", "BType", asJsonNode([])),
                    m.getCacheObject("B3", "BType", asJsonNode([])),
                    m.getCacheObject("C1", "CType", asJsonNode([])),
                    m.getCacheObject("C2", "CType", asJsonNode([])),
                    m.getCacheObject("C3", "CType", asJsonNode([]))
                ] as Set,
                [] as Set,
                true
            )
        )
        
        then: "the CacheChangeSetProcessor on receipt of a cache image CacheChangeSet, makes the pending selector the current selector, filters and outputs a cache image CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("C1", "CType", asJsonNode([])),
                    m.getCacheObject("C2", "CType", asJsonNode([])),
                    m.getCacheObject("C3", "CType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A1"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("A4"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("B3")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A5", "AType", asJsonNode([])),
                        m.getCacheObject("B4", "BType", asJsonNode([])),
                        m.getCacheObject("C4", "CType", asJsonNode([]))
                    ] as Set,
                    [
                        m.getCacheRemove("A3")
                    ] as Set,
                    false
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("C4", "CType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("A5"),
                    m.getCacheRemove("B4")
                ] as Set,
                false
            )
        }
    }

    def "CacheChangeSetProcessor handles receiving more selectors whilst waiting for input subscription to be established"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        then: "on receipt of the selector, the CacheChangeSetProcessor subscribes to its input"
        input.awaitSubscription()
        subscriber.subscription == null
        
        then: "another selector is received whilst the input subscription is pending, which replaces the previous selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("B") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the CacheChangeSetProcessor establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([])),
                        m.getCacheObject("B1", "BType", asJsonNode([])),
                        m.getCacheObject("C1", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    true
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("B1", "BType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A1"),
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
    }
    
    def "CacheChangeSetProcessor continues to output to its subscriber if selectors subscription completes after publishing at least one selector"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        then: "on receipt of the selector, the CacheChangeSetProcessor subscribes to its input"
        input.awaitSubscription()
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the CacheChangeSetProcessor establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([])),
                        m.getCacheObject("B1", "BType", asJsonNode([])),
                        m.getCacheObject("C1", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    true
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }

        then: "the selectors subscription completes"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onComplete()
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor continues to forward the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A3", "AType", asJsonNode([])),
                        m.getCacheObject("B2", "BType", asJsonNode([])),
                        m.getCacheObject("C2", "CType", asJsonNode([]))
                    ] as Set,
                    [
                        m.getCacheRemove("A2")
                    ] as Set,
                    false
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A3", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }
    }

    def "CacheChangeSetProcessor subscriber receives error and input subscription is not made if selectors subscription fails with error before sending a selector"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def error = new RuntimeException("Error with selectors")
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector, but the selectors subscription fails with an error"
        subscriber.expectError()
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onError(error)
        }
        subscriber.subscription == null
        
        then: "on receipt of the error, the CacheChangeSetProcessor does not subscriber to its input, but fails its subscriber with the error"
        subscriber.awaitError()
        subscriber.receivedError == error
        input.subscriber == null
    }
    
    def "CacheChangeSetProcessor subscriber receives error and input subscription is cancelled if selectors subscription fails with error"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        def error = new RuntimeException("Error with selectors")
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        then: "on receipt of the selector, the CacheChangeSetProcessor subscribes to its input"
        input.awaitSubscription()
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the CacheChangeSetProcessor establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        subscriber.expectError()
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([])),
                        m.getCacheObject("B1", "BType", asJsonNode([])),
                        m.getCacheObject("C1", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    true
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }

        then: "the selectors subscription fails with an error"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onError(error)
        }
        
        then: "the CacheChangeSetProcessor cancels the its input subscription"
        inputSubscription.cancelOnRequest {
            input.subscriber.onComplete() // Expecting a JsonCache to send onComplete if cancelled
        }
        
        then: "the CacheChangeSetProcessor fails its subscriber with the error"
        subscriber.awaitError()
        subscriber.receivedError == error
    }
    
    def "CacheChangeSetProcessor subscriber receives error and input subscription is cancelled if selectors subscription fails with error - alternative"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        def error = new RuntimeException("Error with selectors")
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        then: "on receipt of the selector, the CacheChangeSetProcessor subscribes to its input"
        input.awaitSubscription()
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the CacheChangeSetProcessor establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the selectors subscription fails 'out-of-the-blue'"
        selectors.subscriber.onError(error)
        subscriber.expectError()
        
        then: "the CacheChangeSetProcessor cancels the its input subscription"
        inputSubscription.cancelOnRequest {
            input.subscriber.onComplete() // Expecting a JsonCache to send onComplete if cancelled
        }
        
        then: "the CacheChangeSetProcessor fails its subscriber with the error"
        subscriber.awaitError()
        subscriber.receivedError == error
    }
    
    def "CacheChangeSetProcessor subscriber receives error and selectors subscription is cancelled if input subscription fails with error"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        def error = new RuntimeException("Error with input")
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        then: "on receipt of the selector, the CacheChangeSetProcessor subscribes to its input"
        input.awaitSubscription()
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the CacheChangeSetProcessor establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([])),
                        m.getCacheObject("B1", "BType", asJsonNode([])),
                        m.getCacheObject("C1", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    true
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.subscription.request(1)
        subscriber.expectError()
        
        then: "the CacheChangeSetProcessor forwards the request to its input, but the input fails with an error"
        inputSubscription.outputOnRequest {
            input.subscriber.onError(error)
        }
        
        then: "the CacheChangeSetProcessor cancels the selectors subscription"
        selectorsSubscription.cancelOnRequest {
            // Do nothing - a publisher need not do anything on receiving cancel request other than cease sending
        }
        
        then: "the CacheChangeSetProcessor fails its subscriber with the error"
        subscriber.awaitError()
        subscriber.receivedError == error
    }

    def "CacheChangeSetProcessor subscriber receives error and selectors subscription is cancelled if input subscription fails with error - alternative"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        def error = new RuntimeException("Error with input")
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        then: "on receipt of the selector, the CacheChangeSetProcessor subscribes to its input"
        input.awaitSubscription()
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the CacheChangeSetProcessor establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([])),
                        m.getCacheObject("B1", "BType", asJsonNode([])),
                        m.getCacheObject("C1", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    true
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the input fails with an error 'out-of-the-blue' with no request for input pending"
        input.subscriber.onError(error)
        subscriber.expectError()
        
        then: "the CacheChangeSetProcessor cancels the selectors subscription"
        selectorsSubscription.cancelOnRequest {
            // Do nothing - a publisher need not do anything on receiving cancel request other than cease sending
        }
        
        then: "the CacheChangeSetProcessor fails its subscriber with the error"
        subscriber.awaitError()
        subscriber.receivedError == error
    }
    
    @IgnoreRest
    def "CacheChangeSetProcessor subscriber is completed when cancelled and selectors and input subscriptions are cancelled"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        
        when: "a subscription is made to the CacheChangeSetProcessor"
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(input)
        processor.subscribe(subscriber)
        
        then: "the CacheChangeSetProcessor subscribes to the selectors"
        selectors.awaitSubscription()
        subscriber.subscription == null
        
        when: "the selectors subscription is established"
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then: "the CacheChangeSetProcessor requests a selector"
        selectorsSubscription.outputOnRequest {
            selectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        subscriber.subscription == null
        
        then: "on receipt of the selector, the CacheChangeSetProcessor subscribes to its input"
        input.awaitSubscription()
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the CacheChangeSetProcessor establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([])),
                        m.getCacheObject("B1", "BType", asJsonNode([])),
                        m.getCacheObject("C1", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    true
                )
            )
        }
        
        then: "the CacheChangeSetProcessor on receipt of a CacheChangeSet, filters using selector, and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            receivedChangeSet == m.getCacheChangeSet(
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }

        when: "the subscription to the CacheChangeSetProcessor is cancelled"
        subscriber.subscription.cancel()
        
        then: "the CacheChangeSetProcessor cancels the selectors subscription"
        selectorsSubscription.cancelOnRequest {
            // Do nothing - a publisher need not do anything on receiving cancel request other than cease sending
        }
        
        then: "the CacheChangeSetProcessor cancels the its input subscription"
        inputSubscription.cancelOnRequest {
            input.subscriber.onComplete() // Expecting a JsonCache to send onComplete if cancelled
        }
        
        then: "the subscription is completed"
        subscriber.awaitCompleted()
    }
    
    def "CacheChangeSetProcessor forwards call for it to output a cache image to its input"() {
        
    }
    
    private static class MockSubscription implements Subscription {

        private final long timeout
        
        MockSubscription(long timeout = 1000) {
            this.timeout = timeout
        }
        
        private final Object requestMonitor = new Object() 
        private boolean requested

        boolean outputOnRequest(Closure outputCode) {
            synchronized(requestMonitor) {
                if(!requested) {
                    requestMonitor.wait(timeout)
                    if(!requested) return false
                }
                requested = false
            }
            outputCode()
            return true
        }
        
        @Override
        void request(final long n) {
            synchronized(requestMonitor) {
                requested = true
                requestMonitor.notify()
            }
        }

        private final Object cancelRequestMonitor = new Object() 
        private boolean cancelRequested

        boolean cancelOnRequest(Closure cancelCode) {
            synchronized(cancelRequestMonitor) {
                if(!cancelRequested) {
                    cancelRequestMonitor.wait(timeout)
                    if(!cancelRequested) return false
                }
                cancelRequested = false
            }
            cancelCode()
            return true
        }
        
        @Override
        void cancel() {
            synchronized(cancelRequestMonitor) {
                cancelRequested = true
                cancelRequestMonitor.notify()
            }
        }
    }
    
    private static class MockCacheImageSender implements CacheImageSender {

        Subscriber<? super CacheChangeSet> subscriber
        Subscriber<? super CacheChangeSet> sendImageSubscriber
        
        private CountDownLatch subscriptionRequests = new CountDownLatch(1)
        private CountDownLatch sendImageRequests
        
        boolean awaitSubscription(long milliseconds = 1000) {
            subscriptionRequests.await(milliseconds, TimeUnit.MILLISECONDS)
        }

        void expectSendImageRequest() {
            sendImageSubscriber = null
            sendImageRequests = new CountDownLatch(1)
        }
        
        boolean awaitSendImageRequest(long milliseconds = 1000) {
            sendImageRequests.await(milliseconds, TimeUnit.MILLISECONDS)
        }
        
        @Override
        void sendImageToSubscriber(final Subscriber<? super CacheChangeSet> subscriber) {
            sendImageSubscriber = subscriber
            sendImageRequests.countDown()
        }

        @Override
        void subscribe(final Subscriber<? super CacheChangeSet> s) {
            subscriber = s
            subscriptionRequests.countDown()
        }
    }
    
    private static class MockSelectorsPublisher implements Publisher<Predicate<CacheObject>> {

        private CountDownLatch subscriptionRequests = new CountDownLatch(1)
        
        boolean awaitSubscription(long milliseconds = 1000) {
            subscriptionRequests.await(milliseconds, TimeUnit.MILLISECONDS)
        }

        Subscriber<? super Predicate<CacheObject>> subscriber
        
        @Override
        void subscribe(final Subscriber<? super Predicate<CacheObject>> s) {
            subscriber = s
            subscriptionRequests.countDown()
        }
    }
    
    private static class MockSubscriber implements Subscriber<CacheChangeSet>
    {
        Subscription subscription
        
        CacheChangeSet receivedChangeSet
        Throwable receivedError

        private CountDownLatch changeSetReceived
        private final CountDownLatch subscribed = new CountDownLatch(1)
        private final CountDownLatch completed = new CountDownLatch(1)
        private final CountDownLatch errorReceived = new CountDownLatch(1)

        boolean awaitSubscribed(long milliseconds = 1000) {
            subscribed.await(milliseconds, TimeUnit.MILLISECONDS)
        }
        
        void expectChangeSet() {
            receivedChangeSet = null
            changeSetReceived = new CountDownLatch(1)
        }
  
        boolean awaitChangeSet(long milliseconds = 1000) {
            changeSetReceived.await(milliseconds, TimeUnit.MILLISECONDS) 
        }
        
        boolean awaitCompleted(long milliseconds = 1000) {
            completed.await(milliseconds, TimeUnit.MILLISECONDS)
        }

        void expectError() {
            receivedError = null
        }
  
        boolean awaitError(long milliseconds = 1000) {
            errorReceived.await(milliseconds, TimeUnit.MILLISECONDS)
        }
        
        @Override
        void onSubscribe(final Subscription s) {
            subscription = s
            subscribed.countDown()
        }

        @Override
        void onNext(final CacheChangeSet cacheChangeSet) {
            receivedChangeSet = cacheChangeSet
            changeSetReceived.countDown()
        }

        @Override
        void onError(final Throwable t) {
            receivedError = t
            errorReceived.countDown()
        }

        @Override
        void onComplete() {
            completed.countDown()
        }
    }
}
