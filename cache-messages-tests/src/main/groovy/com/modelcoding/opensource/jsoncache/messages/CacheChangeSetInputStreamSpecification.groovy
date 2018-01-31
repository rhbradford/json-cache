// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import com.modelcoding.opensource.jsoncache.messages.CacheChangeSetInputStream.Observer
import com.modelcoding.opensource.jsoncache.messages.testSupport.MockSubscriber
import com.modelcoding.opensource.jsoncache.messages.testSupport.MockSubscription
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
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

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetInputStream observer is notified when subscription is made to a source of CacheMessages"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def observer = Mock(CacheChangeSetInputStream.Observer)
        def cacheMessageSubscription = new MockSubscription()
        cacheMessageSubscription.outputOnRequest {
            throw new IllegalStateException("Should be no request for CacheMessages until CacheChangeSets are demanded")
        }
        
        when:
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(observer)
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then:
        1 * observer.onSubscribed(_)
    }
    
    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetInputStream creates demand for CacheMessages when demand is registered for CacheChangeSets"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def cacheChangeSetSubscriber = new MockSubscriber()
        def cacheMessageSubscription = Mock(Subscription)
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(
            new Observer() {

                @Override
                void onSubscribed(final Publisher<CacheChangeSet> changeSetPublisher) {
                    
                    changeSetPublisher.subscribe(cacheChangeSetSubscriber)
                }
            }
        )

        when: "Subscription is started with source of CacheMessages"
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started, but no requests for CacheMessages is made as yet"
        cacheChangeSetSubscriber.awaitSubscribed()
        0 * cacheMessageSubscription.request(_)
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet"
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "Request is made for a CacheMessage"
        1 * cacheMessageSubscription.request(1)
    }
    
    def "CacheChangeSetInputStream outputs CacheChangeSets built from stream of CacheMessages as expected"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def cacheChangeSetSubscriber = new MockSubscriber()
        def cacheMessageSubscription = new MockSubscription()
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(
            new Observer() {

                @Override
                void onSubscribed(final Publisher<CacheChangeSet> changeSetPublisher) {
                    
                    changeSetPublisher.subscribe(cacheChangeSetSubscriber)
                }
            }
        )
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
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet"
        cacheChangeSetSubscriber.expectChangeSets(1)
        cacheMessageSubscription.expectRequests(cacheMessages1.size())
        cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()) {
                cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "Requests for CacheMessages are made continuously until a CacheChangeSet is assembled and output"
        cacheMessageSubscription.awaitRequests()
        with(cacheChangeSetSubscriber) {
            awaitChangeSets()
            receivedChangeSets.size() == 1
            receivedChangeSets.head() == changeSet1
            receivedChangeSets.head().id == changeSet1.id
            !hasCompleted
            !hasError
        }

        when: "CacheChangeSet subscriber requests more CacheChangeSets"
        cacheChangeSetSubscriber.expectChangeSets(2)
        cacheMessageSubscription.expectRequests(cacheMessages2.size() + cacheMessages3.size())
        cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages2.size()) {
                cacheMessageSubscriber.onNext(cacheMessages2[request])
            } else if(request >= cacheMessages2.size() && request < cacheMessages2.size() + cacheMessages3.size()) {
                cacheMessageSubscriber.onNext(cacheMessages3[request - cacheMessages2.size()])
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        cacheChangeSetSubscriber.subscription.request(2)
        
        then: "Requests for CacheMessages are made continuously until all CacheChangeSets are assembled and output"
        with(cacheChangeSetSubscriber) {
            awaitChangeSets()
            receivedChangeSets.size() == 2
            receivedChangeSets[0] == changeSet2
            receivedChangeSets[0].id == changeSet2.id
            receivedChangeSets[1] == changeSet3
            receivedChangeSets[1].id == changeSet3.id
            !hasCompleted
            !hasError
        }
    }
    
    def "CacheChangeSetInputStream completes CacheChangeSet subscription if source of CacheMessages completes"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def cacheChangeSetSubscriber = new MockSubscriber()
        def cacheMessageSubscription = new MockSubscription()
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(
            new Observer() {

                @Override
                void onSubscribed(final Publisher<CacheChangeSet> changeSetPublisher) {
                    
                    changeSetPublisher.subscribe(cacheChangeSetSubscriber)
                }
            }
        )
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
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "Source of CacheMessages completes"
        cacheMessageSubscriber.onComplete()
        
        then: "CacheChangeSet subscription is completed"
        with(cacheChangeSetSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }
        
        when: 
        cacheMessageSubscriber = new MockSubscriber()
        cacheMessageSubscription = new MockSubscription()
        cacheChangeSetSubscriber = new MockSubscriber()
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then:
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet"
        cacheChangeSetSubscriber.expectChangeSets(1)
        cacheMessageSubscription.expectRequests(cacheMessages1.size())
        cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()) {
                cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "Requests for CacheMessages are made continuously until a CacheChangeSet is assembled and output"
        cacheMessageSubscription.awaitRequests()
        with(cacheChangeSetSubscriber) {
            awaitChangeSets()
            receivedChangeSets.size() == 1
            receivedChangeSets.head() == changeSet1
            receivedChangeSets.head().id == changeSet1.id
            !hasCompleted
            !hasError
        }
        
        when: "Source of CacheMessages completes"
        cacheMessageSubscriber.onComplete()
        
        then: "CacheChangeSet subscription is completed"
        with(cacheChangeSetSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }
        
        when: 
        cacheMessageSubscriber = new MockSubscriber()
        cacheMessageSubscription = new MockSubscription()
        cacheChangeSetSubscriber = new MockSubscriber()
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then:
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet but CacheMessages completes part-way"
        cacheMessageSubscription.expectRequests(cacheMessages1.size())
        cacheMessageSubscription.outputOnRequest { int request ->
            if(request < 3) {
                cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                cacheMessageSubscriber.onComplete()   
            }
        }
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "CacheChangeSet subscription is completed"
        with(cacheChangeSetSubscriber) {
            awaitCompleted()
            receivedChangeSets.size() == 0
            hasCompleted
            !hasError
        }
    }
    
    def "CacheChangeSetInputStream fails CacheChangeSet subscription if source of CacheMessages fails"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def cacheChangeSetSubscriber = new MockSubscriber()
        def cacheMessageSubscription = new MockSubscription()
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(
            new Observer() {

                @Override
                void onSubscribed(final Publisher<CacheChangeSet> changeSetPublisher) {
                    
                    changeSetPublisher.subscribe(cacheChangeSetSubscriber)
                }
            }
        )
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
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "Source of CacheMessages fails"
        cacheChangeSetSubscriber.expectError()
        cacheMessageSubscriber.onError(new RuntimeException("CacheMessage source has failed"))
        
        then: "CacheChangeSet subscription is failed"
        with(cacheChangeSetSubscriber) {
            awaitError()
            !hasCompleted
            hasError
            receivedError.message == "CacheMessage source has failed"
        }
        
        when: 
        cacheMessageSubscriber = new MockSubscriber()
        cacheMessageSubscription = new MockSubscription()
        cacheChangeSetSubscriber = new MockSubscriber()
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then:
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet"
        cacheChangeSetSubscriber.expectChangeSets(1)
        cacheMessageSubscription.expectRequests(cacheMessages1.size())
        cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()) {
                cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "Requests for CacheMessages are made continuously until a CacheChangeSet is assembled and output"
        cacheMessageSubscription.awaitRequests()
        with(cacheChangeSetSubscriber) {
            awaitChangeSets()
            receivedChangeSets.size() == 1
            receivedChangeSets.head() == changeSet1
            receivedChangeSets.head().id == changeSet1.id
            !hasCompleted
            !hasError
        }
        
        when: "Source of CacheMessages fails"
        cacheChangeSetSubscriber.expectError()
        cacheMessageSubscriber.onError(new RuntimeException("CacheMessage source has failed"))
        
        then: "CacheChangeSet subscription is failed"
        with(cacheChangeSetSubscriber) {
            awaitError()
            !hasCompleted
            hasError
            receivedError.message == "CacheMessage source has failed"
        }
        
        when: 
        cacheMessageSubscriber = new MockSubscriber()
        cacheMessageSubscription = new MockSubscription()
        cacheChangeSetSubscriber = new MockSubscriber()
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then:
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet but CacheMessages fails part-way"
        cacheMessageSubscription.expectRequests(cacheMessages1.size())
        cacheMessageSubscription.outputOnRequest { int request ->
            if(request < 3) {
                cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                cacheMessageSubscriber.onError(new RuntimeException("CacheMessage source has failed"))
            }
        }
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "CacheChangeSet subscription is failed"
        with(cacheChangeSetSubscriber) {
            awaitError()
            receivedChangeSets.size() == 0
            !hasCompleted
            hasError
            receivedError.message == "CacheMessage source has failed"
        }
    }
    
    def "CacheChangeSetInputStream fails CacheChangeSet subscription if CacheMessage sequence is incorrect"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def cacheChangeSetSubscriber = new MockSubscriber()
        def cacheMessageSubscription = new MockSubscription()
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(
            new Observer() {

                @Override
                void onSubscribed(final Publisher<CacheChangeSet> changeSetPublisher) {
                    
                    changeSetPublisher.subscribe(cacheChangeSetSubscriber)
                }
            }
        )
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
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet, but the sequence of CacheMessages won't build a CacheChangeSet"
        cacheChangeSetSubscriber.expectError()
        cacheMessageSubscription.expectRequests(cacheMessages1.size())
        cacheMessageSubscription.expectCancel()
        cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()-1) {
                cacheMessageSubscriber.onNext(cacheMessages1[request])
            } else {
                cacheMessageSubscriber.onNext(m.getCacheObject("Bad", "Message", asJsonNode([])))   
            }
        }
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "CacheMessage subscription is cancelled, and the CacheChangeSet subscriber is failed"
        with(cacheMessageSubscription) {
            awaitRequests()
            awaitCancel()
            cancelRequested
        }
        with(cacheChangeSetSubscriber) {
            awaitError()
            receivedChangeSets.size() == 0
            !hasCompleted
            hasError
        }
    }
    
    def "CacheChangeSetInputStream cancels CacheMessage subscription if CacheChangeSet subscription is cancelled"() {
        
        setup:
        CacheChangeSetFrameAssembler assembler = g.getCacheChangeSetFrameAssembler()
        CacheChangeSetInputStream stream = g.getCacheChangeSetInputStream(assembler)
        def cacheChangeSetSubscriber = new MockSubscriber()
        def cacheMessageSubscription = new MockSubscription()
        def cacheMessageSubscriber = stream.getCacheMessageSubscriber(
            new Observer() {

                @Override
                void onSubscribed(final Publisher<CacheChangeSet> changeSetPublisher) {
                    
                    changeSetPublisher.subscribe(cacheChangeSetSubscriber)
                }
            }
        )
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
        cacheMessageSubscriber.onSubscribe(cacheMessageSubscription)
        
        then: "CacheChangeSet subscriber is started"
        cacheChangeSetSubscriber.awaitSubscribed()
        
        when: "CacheChangeSet subscriber requests a CacheChangeSet, and then cancels"
        cacheMessageSubscription.expectRequests(cacheMessages1.size())
        cacheMessageSubscription.expectCancel()
        cacheMessageSubscription.outputOnRequest { int request ->
            if(request < cacheMessages1.size()) {
                cacheMessageSubscriber.onNext(cacheMessages1[request])
                if(request == 2)
                    cacheChangeSetSubscriber.subscription.cancel()
            } else {
                throw new IllegalStateException("Should not request more CacheMessages once demand for CacheChangeSet is fulfilled")   
            }
        }
        cacheChangeSetSubscriber.subscription.request(1)
        
        then: "CacheMessage subscription is cancelled, and the CacheChangeSet subscriber completes"
        with(cacheMessageSubscription) {
            awaitCancel()
            cancelRequested
        }
        with(cacheChangeSetSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }
    }
}
