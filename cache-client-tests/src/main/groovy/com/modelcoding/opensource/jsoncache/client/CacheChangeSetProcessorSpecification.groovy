// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import com.modelcoding.opensource.jsoncache.*
import com.modelcoding.opensource.jsoncache.client.testsupport.MockCacheImageSender
import com.modelcoding.opensource.jsoncache.client.testsupport.MockSelectorsPublisher
import com.modelcoding.opensource.jsoncache.client.testsupport.MockSubscriber
import com.modelcoding.opensource.jsoncache.client.testsupport.MockSubscription
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.*
import spock.lang.Specification

import java.util.function.Predicate

import static com.modelcoding.opensource.jsoncache.client.TestSuite.*

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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
                "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
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
        subscriber.expectError()
        selectors.subscriber.onError(error)
        
        then: "the CacheChangeSetProcessor cancels the its input subscription"
        inputSubscription.cancelOnRequest {
            input.subscriber.onComplete() // Expecting a JsonCache to send onComplete if cancelled
        }
        
        then: "the CacheChangeSetProcessor fails its subscriber with the error"
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
        subscriber.expectError()
        input.subscriber.onError(error)
        
        then: "the CacheChangeSetProcessor cancels the selectors subscription"
        selectorsSubscription.cancelOnRequest {
            // Do nothing - a publisher need not do anything on receiving cancel request other than cease sending
        }
        
        then: "the CacheChangeSetProcessor fails its subscriber with the error"
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
    }
    
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
        with(subscriber) {
            awaitCompleted()
            !hasError
            hasCompleted
        }
    }
    
    def "CacheChangeSetProcessor subscriber is completed when input is completed, and the selectors subscription is cancelled"() {
        
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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

        when: "the input to the CacheChangeSetProcessor is completed"
        input.subscriber.onComplete()
        
        then: "the CacheChangeSetProcessor cancels the selectors subscription"
        selectorsSubscription.cancelOnRequest {
            // Do nothing - a publisher need not do anything on receiving cancel request other than cease sending
        }
        
        then: "the subscription is completed"
        with(subscriber) {
            awaitCompleted()
            !hasError
            hasCompleted
        }
    }
    
    
    def "CacheChangeSetProcessor subscriber receives error when input fails, and the selectors subscription is cancelled"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        def error = new RuntimeException("Error from input")
        
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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

        when: "the input to the CacheChangeSetProcessor fails"
        subscriber.expectError()
        input.subscriber.onError(error)
        
        then: "the CacheChangeSetProcessor cancels the selectors subscription"
        selectorsSubscription.cancelOnRequest {
            // Do nothing - a publisher need not do anything on receiving cancel request other than cease sending
        }
        
        then: "the subscription fails with error"
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
    }
    
    def "CacheChangeSetProcessor forwards call for it to output a cache image to its input"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = new MockSubscription()
        def inputSubscription = new MockSubscription()
        def processor = c.getCacheChangeSetProcessor(selectors)
        
        when: "a subscription is made to the CacheChangeSetProcessor"
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
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
        
        when: "the CacheChangeSetProcessor is requested to output a cache image CacheChangeSet"
        input.expectSendImageRequest()
        processor.sendImageToSubscriber(subscriber)
        
        then: "the CacheChangeSetProcessor requests its input to send a cache image CacheChangeSet"
        input.awaitSendImageRequest()

        when: "the subscriber requests another CacheChangeSet during this time"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the CacheChangeSetProcessor forwards the request to its input as normal"
        inputSubscription.outputOnRequest {
            input.subscriber.onNext(
                m.getCacheChangeSet(
                    "id",
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
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
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
        
        when: "the input sends a cache image"
        subscriber.expectChangeSet()
        input.sendImageSubscriber.onNext(
            m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A3", "AType", asJsonNode([])),
                    m.getCacheObject("B1", "BType", asJsonNode([])),
                    m.getCacheObject("B2", "BType", asJsonNode([])),
                    m.getCacheObject("C1", "CType", asJsonNode([])),
                    m.getCacheObject("C2", "CType", asJsonNode([]))
                ] as Set,
                [] as Set,
                true
            )
        )
        
        then: "the CacheChangeSetProcessor processes the cache image from the input against the selector and sends processed cache image to subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == 'id'
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A3", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C1"),
                    m.getCacheRemove("C2")
                ] as Set,
                true
            )
        }
    }
}
