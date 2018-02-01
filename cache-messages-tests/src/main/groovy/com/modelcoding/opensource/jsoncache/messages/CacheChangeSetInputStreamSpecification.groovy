// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import com.modelcoding.opensource.jsoncache.messages.CacheChangeSetInputStream.Observer
import com.modelcoding.opensource.jsoncache.messages.testSupport.*
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.messages.TestSuite.*

class CacheChangeSetInputStreamSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup

    def "CacheChangeSetInputStream cannot be created from bad parameters"() {
        
        when:
        g.getCacheChangeSetInputStream(null)
        
        then:
        thrown(NullPointerException)
    }

    def "CacheChangeSetInputStream getCacheMessageSubscriber cannot be called with null observer"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)

        when:
        stream.getCacheMessageSubscriber(null)
        
        then:
        thrown(NullPointerException)
    }

    def "CacheChangeSetInputStream getCacheMessageSubscriber cannot be called more than once"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def observer = Mock(CacheChangeSetInputStream.Observer)

        when:
        stream.getCacheMessageSubscriber(observer)
        stream.getCacheMessageSubscriber(observer)
        
        then:
        thrown(IllegalStateException)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetInputStream observer is notified when subscription is made to a source of CacheMessages"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def observer = new MockInputStreamObserver()
        def cacheMessageSubscription = new MockSubscription()
        cacheMessageSubscription.outputOnRequest {
            throw new IllegalStateException("Should be no request for CacheMessages until CacheChangeSets are demanded")
        }
        
        when:
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(observer)
        observer.expectPublisher()
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then:
        observer.awaitPublisher()
    }
    
    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetInputStream creates demand for CacheMessages when demand is registered for CacheChangeSets"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def cacheChangeSetSubscriber = new MockSubscriber<CacheChangeSet>()
        def cacheMessageSubscription = new MockSubscription()
        def observer = new MockInputStreamObserver()
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(observer)

        when: "Subscription is started with source of CacheMessages"
        observer.expectPublisher()
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then: "Publisher of CacheChangeSets delivered to observer" 
        observer.awaitPublisher()
        
        when: "Subscription for CacheChangeSets is made"
        observer.publisher.subscribe(cacheChangeSetSubscriber)
        
        then: "CacheChangeSet subscriber is started"
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet"
        cacheMessageSubscription.expectRequests(1)
        cacheMessageSubscription.outputOnRequest {}
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "Request is made for a CacheMessage"
        cacheMessageSubscription.awaitRequests()
        
        when: "Another subscription for CacheChangeSets is made"
        observer.publisher.subscribe(Mock(Subscriber))
        
        then: "An exception occurs as CacheChangeSet Publisher can only be subscribed to once"
        thrown(Exception)
    }
    
    private class Components {
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def cacheChangeSetSubscriber = new MockSubscriber<CacheChangeSet>()
        def cacheMessageSubscription = new MockSubscription()
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(
            new Observer() {

                @Override
                void onSubscribed(final Publisher<CacheChangeSet> changeSetPublisher) {
                    
                    changeSetPublisher.subscribe(cacheChangeSetSubscriber)
                }
            }
        )
    }
    
    def "CacheChangeSetInputStream outputs CacheChangeSets built from stream of CacheMessages as expected"() {
        
        setup:
        Components c = new Components()
        def changeSet1 = m.getCacheChangeSet(
            "id1", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([])),
                m.getCacheObject("A2", "AType", asJsonNode([])),
                m.getCacheObject("B1", "BType", asJsonNode([])),
                m.getCacheObject("C1", "CType", asJsonNode([]))
            ] as Set, 
            [
                m.getCacheRemove("A3")
            ] as Set, 
            false
        )
        def cacheChangeSetFrame1 = g.getCacheChangeSetFrame(changeSet1)
        def cacheMessages1 = cacheChangeSetFrame1.messages
        def changeSet2 = m.getCacheChangeSet(
            "id2", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([]))
            ] as Set, 
            [
                
            ] as Set, 
            true
        )
        def cacheChangeSetFrame2 = g.getCacheChangeSetFrame(changeSet2)
        def cacheMessages2 = cacheChangeSetFrame2.messages
        def changeSet3 = m.getCacheChangeSet(
            "id3", 
            [
            ] as Set, 
            [
                m.getCacheRemove("A3")
            ] as Set, 
            false
        )
        def cacheChangeSetFrame3 = g.getCacheChangeSetFrame(changeSet3)
        def cacheMessages3 = cacheChangeSetFrame3.messages

        when: "Subscription is started with source of CacheMessages"
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet"
        c.cacheChangeSetSubscriber.expectObjects(1)
        c.cacheMessageSubscription.expectRequests(cacheMessages1.size())
        c.cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()) {
                c.cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        c.cacheChangeSetSubscriber.subscription.request(1)
        
        then: "Requests for CacheMessages are made continuously until a CacheChangeSet is assembled and output"
        c.cacheMessageSubscription.awaitRequests()
        with(c.cacheChangeSetSubscriber) {
            awaitObjects()
            receivedObjects.size() == 1
            receivedObjects.head() == changeSet1
            receivedObjects.head().id == changeSet1.id
            !hasCompleted
            !hasError
        }

        when: "CacheChangeSet subscriber requests more CacheChangeSets"
        c.cacheChangeSetSubscriber.expectObjects(2)
        c.cacheMessageSubscription.expectRequests(cacheMessages2.size() + cacheMessages3.size())
        c.cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages2.size()) {
                c.cacheMessageSubscriber.onNext(cacheMessages2[request])
            } else if(request >= cacheMessages2.size() && request < cacheMessages2.size() + cacheMessages3.size()) {
                c.cacheMessageSubscriber.onNext(cacheMessages3[request - cacheMessages2.size()])
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        c.cacheChangeSetSubscriber.subscription.request(2)
        
        then: "Requests for CacheMessages are made continuously until all CacheChangeSets are assembled and output"
        with(c.cacheChangeSetSubscriber) {
            awaitObjects()
            receivedObjects.size() == 2
            receivedObjects[0] == changeSet2
            receivedObjects[0].id == changeSet2.id
            receivedObjects[1] == changeSet3
            receivedObjects[1].id == changeSet3.id
            !hasCompleted
            !hasError
        }
    }

    def "CacheChangeSetInputStream completes CacheChangeSet subscription if source of CacheMessages completes"() {
        
        setup:
        Components c = new Components()
        def changeSet1 = m.getCacheChangeSet(
            "id1", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([])),
                m.getCacheObject("A2", "AType", asJsonNode([])),
                m.getCacheObject("B1", "BType", asJsonNode([])),
                m.getCacheObject("C1", "CType", asJsonNode([]))
            ] as Set, 
            [
                m.getCacheRemove("A3")
            ] as Set, 
            false
        )
        def cacheChangeSetFrame1 = g.getCacheChangeSetFrame(changeSet1)
        def cacheMessages1 = cacheChangeSetFrame1.messages

        when: "Subscription is started with source of CacheMessages"
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "Source of CacheMessages completes"
        c.cacheMessageSubscriber.onComplete()
        
        then: "CacheChangeSet subscription is completed"
        with(c.cacheChangeSetSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }
        
        when: 
        c = new Components()
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then:
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet"
        c.cacheChangeSetSubscriber.expectObjects(1)
        c.cacheMessageSubscription.expectRequests(cacheMessages1.size())
        c.cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()) {
                c.cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        c.cacheChangeSetSubscriber.subscription.request(1)
        
        then: "Requests for CacheMessages are made continuously until a CacheChangeSet is assembled and output"
        c.cacheMessageSubscription.awaitRequests()
        with(c.cacheChangeSetSubscriber) {
            awaitObjects()
            receivedObjects.size() == 1
            receivedObjects.head() == changeSet1
            receivedObjects.head().id == changeSet1.id
            !hasCompleted
            !hasError
        }
        
        when: "Source of CacheMessages completes"
        c.cacheMessageSubscriber.onComplete()
        
        then: "CacheChangeSet subscription is completed"
        with(c.cacheChangeSetSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }
        
        when: 
        c = new Components()
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then:
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet but CacheMessages completes part-way"
        c.cacheMessageSubscription.expectRequests(cacheMessages1.size())
        c.cacheMessageSubscription.outputOnRequest { int request ->
            if(request < 3) {
                c.cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                c.cacheMessageSubscriber.onComplete()   
            }
        }
        c.cacheChangeSetSubscriber.subscription.request(1)
        
        then: "CacheChangeSet subscription is completed"
        with(c.cacheChangeSetSubscriber) {
            awaitCompleted()
            receivedObjects.size() == 0
            hasCompleted
            !hasError
        }
    }
    
    def "CacheChangeSetInputStream fails CacheChangeSet subscription if source of CacheMessages fails"() {
        
        setup:
        Components c = new Components()
        def changeSet1 = m.getCacheChangeSet(
            "id1", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([])),
                m.getCacheObject("A2", "AType", asJsonNode([])),
                m.getCacheObject("B1", "BType", asJsonNode([])),
                m.getCacheObject("C1", "CType", asJsonNode([]))
            ] as Set, 
            [
                m.getCacheRemove("A3")
            ] as Set, 
            false
        )
        def cacheChangeSetFrame1 = g.getCacheChangeSetFrame(changeSet1)
        def cacheMessages1 = cacheChangeSetFrame1.messages

        when: "Subscription is started with source of CacheMessages"
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "Source of CacheMessages fails"
        c.cacheChangeSetSubscriber.expectError()
        c.cacheMessageSubscriber.onError(new RuntimeException("CacheMessage source has failed"))
        
        then: "CacheChangeSet subscription is failed"
        with(c.cacheChangeSetSubscriber) {
            awaitError()
            !hasCompleted
            hasError
            receivedError.message == "CacheMessage source has failed"
        }
        
        when:
        c = new Components()
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then:
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet"
        c.cacheChangeSetSubscriber.expectObjects(1)
        c.cacheMessageSubscription.expectRequests(cacheMessages1.size())
        c.cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()) {
                c.cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        c.cacheChangeSetSubscriber.subscription.request(1)
        
        then: "Requests for CacheMessages are made continuously until a CacheChangeSet is assembled and output"
        c.cacheMessageSubscription.awaitRequests()
        with(c.cacheChangeSetSubscriber) {
            awaitObjects()
            receivedObjects.size() == 1
            receivedObjects.head() == changeSet1
            receivedObjects.head().id == changeSet1.id
            !hasCompleted
            !hasError
        }
        
        when: "Source of CacheMessages fails"
        c.cacheChangeSetSubscriber.expectError()
        c.cacheMessageSubscriber.onError(new RuntimeException("CacheMessage source has failed"))
        
        then: "CacheChangeSet subscription is failed"
        with(c.cacheChangeSetSubscriber) {
            awaitError()
            !hasCompleted
            hasError
            receivedError.message == "CacheMessage source has failed"
        }
        
        when: 
        c = new Components()
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then:
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet but CacheMessages fails part-way"
        c.cacheMessageSubscription.expectRequests(cacheMessages1.size())
        c.cacheMessageSubscription.outputOnRequest { int request ->
            if(request < 3) {
                c.cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                c.cacheMessageSubscriber.onError(new RuntimeException("CacheMessage source has failed"))
            }
        }
        c.cacheChangeSetSubscriber.subscription.request(1)
        
        then: "CacheChangeSet subscription is failed"
        with(c.cacheChangeSetSubscriber) {
            awaitError()
            receivedObjects.size() == 0
            !hasCompleted
            hasError
            receivedError.message == "CacheMessage source has failed"
        }
    }
    
    def "CacheChangeSetInputStream fails CacheChangeSet subscription if CacheMessage sequence is incorrect"() {
        
        setup:
        Components c = new Components()
        def changeSet1 = m.getCacheChangeSet(
            "id1", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([])),
                m.getCacheObject("A2", "AType", asJsonNode([])),
                m.getCacheObject("B1", "BType", asJsonNode([])),
                m.getCacheObject("C1", "CType", asJsonNode([]))
            ] as Set, 
            [
                m.getCacheRemove("A3")
            ] as Set, 
            false
        )
        def cacheChangeSetFrame1 = g.getCacheChangeSetFrame(changeSet1)
        def cacheMessages1 = cacheChangeSetFrame1.messages

        when: "Subscription is started with source of CacheMessages"
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet, but the sequence of CacheMessages won't build a CacheChangeSet"
        c.cacheChangeSetSubscriber.expectError()
        c.cacheMessageSubscription.expectRequests(cacheMessages1.size())
        c.cacheMessageSubscription.expectCancel()
        c.cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()-1) {
                c.cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                c.cacheMessageSubscriber.onNext(m.getCacheObject("Bad", "Message", asJsonNode([])))   
            }
        }
        c.cacheChangeSetSubscriber.subscription.request(1)
        
        then: "CacheMessage subscription is cancelled, and the CacheChangeSet subscriber is failed"
        with(c.cacheMessageSubscription) {
            awaitRequests()
            awaitCancel()
            cancelRequested
        }
        with(c.cacheChangeSetSubscriber) {
            awaitError()
            receivedObjects.size() == 0
            !hasCompleted
            hasError
        }
    }
    
    def "CacheChangeSetInputStream cancels CacheMessage subscription if CacheChangeSet subscription is cancelled"() {
        
        setup:
        Components c = new Components()
        def changeSet1 = m.getCacheChangeSet(
            "id1", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([])),
                m.getCacheObject("A2", "AType", asJsonNode([])),
                m.getCacheObject("B1", "BType", asJsonNode([])),
                m.getCacheObject("C1", "CType", asJsonNode([]))
            ] as Set, 
            [
                m.getCacheRemove("A3")
            ] as Set, 
            false
        )
        def cacheChangeSetFrame1 = g.getCacheChangeSetFrame(changeSet1)
        def cacheMessages1 = cacheChangeSetFrame1.messages

        when: "Subscription is started with source of CacheMessages"
        c.cacheMessageSubscriber.onSubscribe(c.cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        c.cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet, and then cancels"
        c.cacheMessageSubscription.expectRequests(cacheMessages1.size())
        c.cacheMessageSubscription.expectCancel()
        c.cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()) {
                c.cacheMessageSubscriber.onNext(cacheMessages1[request])
                if(request == 2)
                    c.cacheChangeSetSubscriber.subscription.cancel()
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        c.cacheChangeSetSubscriber.subscription.request(1)
        
        then: "CacheMessage subscription is cancelled, and the CacheChangeSet subscriber completes"
        with(c.cacheMessageSubscription) {
            awaitCancel()
            cancelRequested
        }
        with(c.cacheChangeSetSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }
    }
}
