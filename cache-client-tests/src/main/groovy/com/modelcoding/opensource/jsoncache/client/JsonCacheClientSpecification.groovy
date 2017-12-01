// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import com.modelcoding.opensource.jsoncache.CacheImageSender
import com.modelcoding.opensource.jsoncache.CacheObject
import com.modelcoding.opensource.jsoncache.client.testsupport.MockCacheImageSender
import com.modelcoding.opensource.jsoncache.client.testsupport.MockSelectorsPublisher
import com.modelcoding.opensource.jsoncache.client.testsupport.MockSubscriber
import com.modelcoding.opensource.jsoncache.client.testsupport.MockSubscription
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.Subscriber
import spock.lang.Specification

import java.util.function.Predicate

import static com.modelcoding.opensource.jsoncache.client.TestSuite.*

class JsonCacheClientSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup

    def "JsonCacheClient is created as expected"() {
        
        when:
        def client = c.getJsonCacheClient("id", Mock(CacheImageSender), Mock(CacheChangeSetProcessor), Mock(CacheChangeSetProcessor))
        
        then:
        client.id == "id"
    }
    
    def "Cannot create a JsonCacheClient from bad parameters"() {

        when:
        c.getJsonCacheClient(null, Mock(CacheImageSender), Mock(CacheChangeSetProcessor), Mock(CacheChangeSetProcessor))
        
        then:
        thrown(NullPointerException)

        when:
        c.getJsonCacheClient("id", null, Mock(CacheChangeSetProcessor), Mock(CacheChangeSetProcessor))
        
        then:
        thrown(NullPointerException)

        when:
        c.getJsonCacheClient("id", Mock(CacheImageSender), null, Mock(CacheChangeSetProcessor))
        
        then:
        thrown(NullPointerException)

        when:
        c.getJsonCacheClient("id", Mock(CacheImageSender), Mock(CacheChangeSetProcessor), null)
        
        then:
        thrown(NullPointerException)
    }
    
    def "JsonCacheClient cannot be subscribed to with bad parameters"() {

        setup:
        def client = c.getJsonCacheClient("id", Mock(CacheImageSender), Mock(CacheChangeSetProcessor), Mock(CacheChangeSetProcessor))

        when:
        client.subscribe(null)

        then:
        thrown(NullPointerException)
    }
    
    def "JsonCacheClient cannot be subscribed to twice"() {
        
        setup:
        def client = c.getJsonCacheClient("id", Mock(CacheImageSender), Mock(CacheChangeSetProcessor), Mock(CacheChangeSetProcessor))

        when:
        client.subscribe(Mock(Subscriber))
        client.subscribe(Mock(Subscriber))
        
        then:
        thrown(IllegalStateException)
    }
    
    def "JsonCacheClient operates as expected given an object selector and an object authorisor"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def inputSubscription = new MockSubscription()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input"
        input.awaitSubscription()
        input.subscriber != null
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the JsonCacheClient establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }
    }
    
    def "JsonCacheClient operates as expected when the object selector is changed"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def inputSubscription = new MockSubscription()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input"
        input.awaitSubscription()
        input.subscriber != null
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the JsonCacheClient establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }
        
        when: "the object selector in the JsonCacheClient changes"
        input.expectSendImageRequest()
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("B") } as Predicate<CacheObject>)
        }
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the output from the JsonCacheClient alters as expected once a cache image is sent in by the input"
        input.awaitSendImageRequest()
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
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("B1", "BType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A1"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C1"),
                    m.getCacheRemove("C2")
                ] as Set,
                true
            )
        }
    }
    
    def "JsonCacheClient operates as expected when the object authorisor is changed"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def inputSubscription = new MockSubscription()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input"
        input.awaitSubscription()
        input.subscriber != null
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the JsonCacheClient establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }
        
        when: "the object authorisor in the JsonCacheClient changes"
        input.expectSendImageRequest()
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("2") } as Predicate<CacheObject>)
        }
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the output from the JsonCacheClient alters as expected once a cache image is sent in by the input"
        input.awaitSendImageRequest()
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
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A1"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C1"),
                    m.getCacheRemove("C2")
                ] as Set,
                true
            )
        }
    }
    
    def "JsonCacheClient subscriber receives error when the selectors fails, and the input and authorisors subscriptions are cancelled"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def inputSubscription = new MockSubscription()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        def error = new RuntimeException("Error with selectors")
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input"
        input.awaitSubscription()
        input.subscriber != null
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the JsonCacheClient establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }

        when: "the selectors subscription fails"
        subscriber.expectError()
        objectSelectors.subscriber.onError(error)
        
        then: "the JsonCacheClient subscriber receives the error, and the input and authorisors subscriptions are cancelled"
        inputSubscription.cancelOnRequest {
            input.subscriber.onComplete() // Emulates JsonCache behaviour
        }
        objectAuthorisorsSubscription.cancelOnRequest {
            // Do nothing when cancelled - an alternative allowed by Publisher specification
        }
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
    }
    
    def "JsonCacheClient subscriber receives error when the authorisors fails, and the input and selectors subscriptions are cancelled"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def inputSubscription = new MockSubscription()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        def error = new RuntimeException("Error with authorisors")
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input"
        input.awaitSubscription()
        input.subscriber != null
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the JsonCacheClient establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }

        when: "the authorisors subscription fails"
        subscriber.expectError()
        objectAuthorisors.subscriber.onError(error)
        
        then: "the JsonCacheClient subscriber receives the error, and the input and selectors subscriptions are cancelled"
        inputSubscription.cancelOnRequest {
            input.subscriber.onComplete() // Emulates JsonCache behaviour
        }
        objectSelectorsSubscription.cancelOnRequest {
            // Do nothing when cancelled - an alternative allowed by Publisher specification
        }
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
    }
    
    def "JsonCacheClient subscriber receives error when the input fails, and the selectors and authorisors subscriptions are cancelled"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def inputSubscription = new MockSubscription()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        def error = new RuntimeException("Error with input")
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input"
        input.awaitSubscription()
        input.subscriber != null
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the JsonCacheClient establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }

        when: "the input subscription fails"
        subscriber.expectError()
        input.subscriber.onError(error)
        
        then: "the JsonCacheClient subscriber receives the error, and the selectors and authorisors subscriptions are cancelled"
        objectAuthorisorsSubscription.cancelOnRequest {
            // Do nothing when cancelled - an alternative allowed by Publisher specification
        }
        objectSelectorsSubscription.cancelOnRequest {
            // Do nothing when cancelled - an alternative allowed by Publisher specification
        }
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
        }
    }
    
    def "JsonCacheClient subscriber is completed when the input is completed, and the selectors and authorisors subscriptions are cancelled"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def inputSubscription = new MockSubscription()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input"
        input.awaitSubscription()
        input.subscriber != null
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the JsonCacheClient establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }

        when: "the input subscription completes"
        input.subscriber.onComplete()
        
        then: "the JsonCacheClient subscriber receives the error, and the selectors and authorisors subscriptions are cancelled"
        objectAuthorisorsSubscription.cancelOnRequest {
            // Do nothing when cancelled - an alternative allowed by Publisher specification
        }
        objectSelectorsSubscription.cancelOnRequest {
            // Do nothing when cancelled - an alternative allowed by Publisher specification
        }
        with(subscriber) {
            awaitCompleted()
            !hasError
            hasCompleted
        }
    }
    
    def "JsonCacheClient subscriber is completed when cancelled, and the input, selectors and authorisors subscriptions are cancelled"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def inputSubscription = new MockSubscription()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input"
        input.awaitSubscription()
        input.subscriber != null
        subscriber.subscription == null
        
        when: "the input subscription is established"
        input.subscriber.onSubscribe(inputSubscription)
        
        then: "the JsonCacheClient establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
        
        when: "the subscriber requests another CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the JsonCacheClient forwards the request to its input"
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
        
        then: "the JsonCacheClient on receipt of a CacheChangeSet, passes it through its processors and outputs a CacheChangeSet to the subscriber"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A3"),
                    m.getCacheRemove("B2"),
                    m.getCacheRemove("C2")
                ] as Set,
                false
            )
        }

        when: "the subscriber cancels its subscription"
        subscriber.subscription.cancel()
        
        then: "the JsonCacheClient subscriber completes, and the input, selectors and authorisors subscriptions are cancelled"
        inputSubscription.cancelOnRequest {
            input.subscriber.onComplete() // Emulate JsonCache behaviour
        }
        objectAuthorisorsSubscription.cancelOnRequest {
            // Do nothing when cancelled - an alternative allowed by Publisher specification
        }
        objectSelectorsSubscription.cancelOnRequest {
            // Do nothing when cancelled - an alternative allowed by Publisher specification
        }
        with(subscriber) {
            awaitCompleted()
            !hasError
            hasCompleted
        }
    }
    
    def "JsonCacheClient subscriber is completed and no input or selectors subscriptions occur if authorisors completes without publishing an authorisor"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            // Do nothing
        }
        
        when: "the authorisors subscription completes without an authorisor being published"
        objectAuthorisors.subscriber.onComplete()
        
        then: "the subscriber completes, and no input or selectors subscriptions occur" 
        with(subscriber) {
            awaitCompleted()
            !hasError
            hasCompleted
            subscription == null
        }
        objectSelectors.subscriber == null
        input.subscriber == null
    }
    
    def "JsonCacheClient subscriber receives error and no input or selectors subscriptions occur if authorisors fails without publishing an authorisor"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        def error = new RuntimeException("Error with authorisor")
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            // Do nothing
        }
        
        when: "the authorisors subscription fails without an authorisor being published"
        subscriber.expectError()
        objectAuthorisors.subscriber.onError(error)
        
        then: "the subscriber receives an error, and no input or selectors subscriptions occur" 
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
            subscription == null
        }
        objectSelectors.subscriber == null
        input.subscriber == null
    }
    
    def "JsonCacheClient subscriber is completed, the authorisors subscription is cancelled and no input subscription occurs if selectors completes without publishing a selector"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            // Do nothing
        }
        
        when: "the selectors subscription completes without a selector being published"
        objectSelectors.subscriber.onComplete()
        
        then: "the subscriber completes, the authorisors subscription is cancelled and no input subscription occurs" 
        with(subscriber) {
            awaitCompleted()
            !hasError
            hasCompleted
            subscription == null
        }
        objectAuthorisorsSubscription.cancelOnRequest {
            // This is checking that a cancel is generated
        }
        input.subscriber == null
    }
    
    def "JsonCacheClient subscriber receives error, the authorisors subscription is cancelled and no input subscription occurs if selectors fails without publishing a selector"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def client = c.getJsonCacheClient("id", input, objectSelectionProcessor, objectAuthorisorProcessor)
        def error = new RuntimeException("Error with selectors")
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        objectAuthorisors.subscriber != null
        objectSelectors.subscriber == null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        objectSelectors.subscriber != null
        input.subscriber == null
        subscriber.subscription == null
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            // Do nothing
        }
        
        when: "the selectors subscription fails without a selector being published"
        subscriber.expectError()
        objectSelectors.subscriber.onError(error)
        
        then: "the subscriber receives an error, the authorisors subscription is cancelled and no input subscription occurs" 
        with(subscriber) {
            awaitError()
            hasError
            !hasCompleted
            receivedError == error
            subscription == null
        }
        objectAuthorisorsSubscription.cancelOnRequest {
            // This is checking that a cancel is generated
        }
        input.subscriber == null
    }
    
    def "JsonCacheClient acts as client to a JsonCache as expected"() {
        
        setup:
        def objectSelectors = new MockSelectorsPublisher()
        def objectSelectorsSubscription = new MockSubscription()
        def objectSelectionProcessor = c.getCacheChangeSetProcessor(objectSelectors)
        def objectAuthorisors = new MockSelectorsPublisher()
        def objectAuthorisorsSubscription = new MockSubscription()
        def objectAuthorisorProcessor = c.getCacheChangeSetProcessor(objectAuthorisors)
        def input = m.getJsonCache("cacheId", 2, m.getCache([] as Set))
        def subscriber = new MockSubscriber()
        def client = c.getJsonCacheClient("cacheClientId", input, objectSelectionProcessor, objectAuthorisorProcessor)
        
        when: "a subscription is made to the JsonCacheClient"
        client.subscribe(subscriber)
        
        then: "the JsonCacheClient wires together its processors and input, and subscribes first to the object authorisors"
        objectAuthorisors.awaitSubscription()
        
        when: "the object authorisors subscription is established"
        objectAuthorisors.subscriber.onSubscribe(objectAuthorisorsSubscription)
        
        then: "the JsonCacheClient requests an authorisor"
        objectAuthorisorsSubscription.outputOnRequest {
            objectAuthorisors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.endsWith("1") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of an authorisor, the JsonCacheClient subscribes to the object selectors"
        objectSelectors.awaitSubscription()
        
        when: "the object selectors subscription is established"
        objectSelectors.subscriber.onSubscribe(objectSelectorsSubscription)
        
        then: "the JsonCacheClient requests a selector"
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>)
        }
        
        then: "on receipt of a selector, the JsonCacheClient subscribes to its input, and then establishes a subscription with its subscriber"
        subscriber.awaitSubscribed()
        subscriber.subscription != null
        
        when: "the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        
        then: "the subscriber initially receives a cache image CacheChangeSet"
        subscriber.awaitChangeSet()
        subscriber.receivedChangeSet == m.getCacheChangeSet(
            "id",
            [] as Set,
            [] as Set,
            true
        )
        
        when: "a change is made to the JsonCache, and the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        input.onNext(
            m.getCacheChangeCalculator(
                m.getCacheChangeSet(
                    "id",
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([])),
                        m.getCacheObject("B1", "BType", asJsonNode([])),
                        m.getCacheObject("C1", "CType", asJsonNode([]))
                    ] as Set,
                    [] as Set,
                    false
                )
            )
        )

        then: "the subscriber receives a CacheChangeSet detailing the changes, as viewed according to the selector and authorisor"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet.id == "id"
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("A1", "AType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("B1"),
                    m.getCacheRemove("C1")
                ] as Set,
                false
            )
        }
        
        when: "the selector is changed, and the subscriber requests a CacheChangeSet"
        subscriber.expectChangeSet()
        subscriber.subscription.request(1)
        objectSelectorsSubscription.outputOnRequest {
            objectSelectors.subscriber.onNext({ CacheObject cacheObject -> cacheObject.id.startsWith("B") } as Predicate<CacheObject>)
        }
        
        then: "the subscriber receives a CacheChangeSet showing a change in the view according to the selector and authorisor"
        with(subscriber) {
            awaitChangeSet()
            !hasError
            !hasCompleted
            receivedChangeSet == m.getCacheChangeSet(
                "id",
                [
                    m.getCacheObject("B1", "BType", asJsonNode([]))
                ] as Set,
                [
                    m.getCacheRemove("A2"),
                    m.getCacheRemove("A1"),
                    m.getCacheRemove("C1")
                ] as Set,
                true
            )
        }
    }
}
