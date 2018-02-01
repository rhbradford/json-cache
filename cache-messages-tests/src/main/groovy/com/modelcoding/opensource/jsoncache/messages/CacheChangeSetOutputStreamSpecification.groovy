// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import com.modelcoding.opensource.jsoncache.CacheMessage
import com.modelcoding.opensource.jsoncache.messages.CacheChangeSetOutputStream.Observer
import com.modelcoding.opensource.jsoncache.messages.testSupport.*
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.messages.TestSuite.*

class CacheChangeSetOutputStreamSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup

    def "CacheChangeSetOutputStream getCacheChangeSetSubscriber cannot be called with null observer"() {
        
        setup:
        CacheChangeSetOutputStream stream = g.cacheChangeSetOutputStream

        when:
        stream.getCacheChangeSetSubscriber(null)
        
        then:
        thrown(NullPointerException)
    }

    def "CacheChangeSetOutputStream getCacheChangeSetSubscriber cannot be called more than once"() {
        
        setup:
        CacheChangeSetOutputStream stream = g.cacheChangeSetOutputStream
        def observer = Mock(CacheChangeSetOutputStream.Observer)

        when:
        stream.getCacheChangeSetSubscriber(observer)
        stream.getCacheChangeSetSubscriber(observer)
        
        then:
        thrown(IllegalStateException)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetOutputStream observer is notified when subscription is made to a source of CacheChangeSets"() {
        
        setup:
        CacheChangeSetOutputStream stream = g.cacheChangeSetOutputStream
        def observer = new MockOutputStreamObserver()
        def cacheChangeSetSubscription = new MockSubscription()
        cacheChangeSetSubscription.outputOnRequest {
            throw new IllegalStateException("Should be no request for CacheChangeSets until CacheMessages are demanded")
        }
        
        when:
        def cacheChangeSetSubscriber = stream.getCacheChangeSetSubscriber(observer)
        observer.expectPublisher()
        cacheChangeSetSubscriber.onSubscribe(cacheChangeSetSubscription)
        
        then:
        observer.awaitPublisher()
    }
    
    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetOutputStream creates demand for CacheChangeSets when demand is registered for CacheMessages"() {
        
        setup:
        CacheChangeSetOutputStream stream = g.cacheChangeSetOutputStream
        def cacheMessageSubscriber = new MockSubscriber()
        def cacheChangeSetSubscription = new MockSubscription()
        def observer = new MockOutputStreamObserver()
        def cacheChangeSetSubscriber = stream.getCacheChangeSetSubscriber(observer)

        when: "Subscription is started with source of CacheChangeSets"
        observer.expectPublisher()
        cacheChangeSetSubscriber.onSubscribe(cacheChangeSetSubscription)
        
        then: "Publisher of CacheMessages delivered to observer" 
        observer.awaitPublisher()
        
        when: "Subscription for CacheMessages is made"
        observer.publisher.subscribe(cacheMessageSubscriber)
        
        then: "CacheMessages subscriber is started"
        cacheMessageSubscriber.awaitSubscribed()
        
        when: "CacheMessages subscriber requests a CacheMessage"
        cacheChangeSetSubscription.expectRequests(1)
        cacheChangeSetSubscription.outputOnRequest {}
        cacheMessageSubscriber.subscription.request(1)
        
        then: "Request is made for a CacheChangeSet"
        cacheChangeSetSubscription.awaitRequests()
        
        when: "Another subscription for CacheMessages is made"
        observer.publisher.subscribe(Mock(Subscriber))
        
        then: "An exception occurs as CacheMessage Publisher can only be subscribed to once"
        thrown(Exception)
    }
    
    private class Components {
        CacheChangeSetOutputStream stream = g.cacheChangeSetOutputStream
        def cacheMessageSubscriber = new MockSubscriber<CacheMessage>()
        def cacheChangeSetSubscription = new MockSubscription()
        def cacheChangeSetSubscriber = stream.getCacheChangeSetSubscriber(
            new Observer() {

                @Override
                void onSubscribed(final Publisher<CacheMessage> cacheMessagePublisher) {
                    
                    cacheMessagePublisher.subscribe(cacheMessageSubscriber)
                }
            }
        )
    }
    
    def "CacheChangeSetOutputStream outputs CacheMessages from stream of CacheChangeSets as expected"() {
        
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

        when: "Subscription is started with source of CacheChangeSets"
        c.cacheChangeSetSubscriber.onSubscribe(c.cacheChangeSetSubscription)
        
        then: "CacheMessages subscriber is started"
        c.cacheMessageSubscriber.awaitSubscribed()
        
        when: "CacheMessages subscriber requests a CacheMessage"
        c.cacheMessageSubscriber.expectObjects(1)
        c.cacheChangeSetSubscription.expectRequests(1)
        c.cacheChangeSetSubscription.outputOnRequest { int request ->
            if(request < 1) {
                c.cacheChangeSetSubscriber.onNext(changeSet1)
            } else {
                throw new IllegalStateException("Should not request more CacheChangeSets if demand for CacheMessages is fulfilled")   
            }
        }
        c.cacheMessageSubscriber.subscription.request(1)
        
        then: "A CacheChangeSet is requested, and a StartOfCacheChangeSet CacheMessage is output"
        c.cacheChangeSetSubscription.awaitRequests()
        with(c.cacheMessageSubscriber) {
            awaitObjects()
            receivedObjects.size() == 1
            receivedObjects.head() == cacheMessages1.head()
            !hasCompleted
            !hasError
        }

        when: "CacheMessages subscriber requests just enough CacheMessages to consume the remainder from the CacheChangeSet"
        c.cacheChangeSetSubscription.outputOnRequest { int request ->
            throw new IllegalStateException("Should not request more CacheChangeSets if demand for CacheMessages is fulfilled")   
        }
        c.cacheMessageSubscriber.expectObjects(cacheMessages1.size()-1)
        c.cacheMessageSubscriber.subscription.request(cacheMessages1.size()-1)

        then: "Further CacheMessages are output until all messages for CacheChangeSet are delivered, and no more CacheChangeSets are requested"
        with(c.cacheMessageSubscriber) {
            awaitObjects()
            receivedObjects == cacheMessages1.tail()
            !hasCompleted
            !hasError
        }
        c.cacheChangeSetSubscription.requestCount == 1

        when: "CacheMessages subscriber requests more CacheMessages to consume next two CacheChangeSets"
        c.cacheMessageSubscriber.expectObjects(cacheMessages2.size() + cacheMessages3.size())
        c.cacheChangeSetSubscription.expectRequests(2)
        c.cacheChangeSetSubscription.outputOnRequest { int request ->
            if(request < 1) {
                c.cacheChangeSetSubscriber.onNext(changeSet2)
            } else if(request < 2) {
                c.cacheChangeSetSubscriber.onNext(changeSet3)
            } else {
                throw new IllegalStateException("Should not request more CacheChangeSets if demand for CacheMessages is fulfilled")   
            }
        }
        c.cacheMessageSubscriber.subscription.request(cacheMessages2.size() + cacheMessages3.size())
        
        then: "Requests for CacheChangeSets are made as appropriate until all messages for CacheChangeSets are delivered"
        with(c.cacheMessageSubscriber) {
            awaitObjects()
            receivedObjects == [cacheMessages2, cacheMessages3].flatten()
            !hasCompleted
            !hasError
        }
        c.cacheChangeSetSubscription.awaitRequests()
        c.cacheChangeSetSubscription.requestCount == 2
    }

    def "CacheChangeSetOutputStream completes CacheMessage subscription if source of CacheChangeSets completes"() {

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

        when: "Subscription is started with source of CacheChangeSets"
        c.cacheChangeSetSubscriber.onSubscribe(c.cacheChangeSetSubscription)

        then: "CacheMessage subscriber is started"
        c.cacheMessageSubscriber.awaitSubscribed()

        when: "Source of CacheChangeSets completes"
        c.cacheChangeSetSubscriber.onComplete()

        then: "CacheMessage subscription is completed"
        with(c.cacheMessageSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }

        when: 
        c = new Components()
        c.cacheChangeSetSubscriber.onSubscribe(c.cacheChangeSetSubscription)

        then:
        c.cacheMessageSubscriber.awaitSubscribed()

        when: "CacheMessage subscriber requests a CacheMessage"
        c.cacheMessageSubscriber.expectObjects(cacheMessages1.size())
        c.cacheChangeSetSubscription.expectRequests(1)
        c.cacheChangeSetSubscription.outputOnRequest { int request ->
            if(request < 1) {
                c.cacheChangeSetSubscriber.onNext(changeSet1)
            } else {
                throw new IllegalStateException("Should not request more CacheChangeSets if demand for CacheMessages is fulfilled")   
            }
        }
        c.cacheMessageSubscriber.subscription.request(cacheMessages1.size())

        then: "CacheMessages are delivered until all for a CacheChangeSet have been output"
        c.cacheChangeSetSubscription.awaitRequests()
        with(c.cacheMessageSubscriber) {
            awaitObjects()
            receivedObjects == cacheMessages1
            !hasCompleted
            !hasError
        }

        when: "Source of CacheChangeSets completes"
        c.cacheChangeSetSubscriber.onComplete()

        then: "CacheMessage subscription is completed"
        with(c.cacheMessageSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }

        when: 
        c = new Components()
        c.cacheChangeSetSubscriber.onSubscribe(c.cacheChangeSetSubscription)

        then:
        c.cacheMessageSubscriber.awaitSubscribed()

        when: "CacheMessages subscriber requests CacheMessages but CacheChangeSets completes part-way"
        c.cacheChangeSetSubscription.expectRequests(2)
        c.cacheChangeSetSubscription.outputOnRequest { int request ->
            if(request < 1)
                c.cacheChangeSetSubscriber.onNext(changeSet2)
            else
                c.cacheChangeSetSubscriber.onComplete()   
        }
        c.cacheMessageSubscriber.subscription.request(cacheMessages2.size()+cacheMessages3.size())

        then: "CacheMessage subscription is completed"
        with(c.cacheMessageSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }
    }

    
    def "CacheChangeSetOutputStream fails CacheMessage subscription if source of CacheChangeSets fails"() {

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

        when: "Subscription is started with source of CacheChangeSets"
        c.cacheChangeSetSubscriber.onSubscribe(c.cacheChangeSetSubscription)

        then: "CacheMessage subscriber is started"
        c.cacheMessageSubscriber.awaitSubscribed()

        when: "Source of CacheChangeSets fails"
        c.cacheMessageSubscriber.expectError()
        c.cacheChangeSetSubscriber.onError(new RuntimeException("CacheChangeSets source has failed"))

        then: "CacheMessage subscription is failed"
        with(c.cacheMessageSubscriber) {
            awaitError()
            !hasCompleted
            hasError
            receivedError.message == "CacheChangeSets source has failed"
        }

        when:
        c = new Components()
        c.cacheChangeSetSubscriber.onSubscribe(c.cacheChangeSetSubscription)

        then:
        c.cacheMessageSubscriber.awaitSubscribed()

        when: "CacheMessage subscriber requests CacheMessages"
        c.cacheMessageSubscriber.expectObjects(cacheMessages1.size())
        c.cacheChangeSetSubscription.expectRequests(1)
        c.cacheChangeSetSubscription.outputOnRequest { int request ->
            if(request < 1) {
                c.cacheChangeSetSubscriber.onNext(changeSet1)
            } else {
                throw new IllegalStateException("Should not request more CacheChangeSets if demand for CacheMessages is fulfilled")   
            }
        }
        c.cacheMessageSubscriber.subscription.request(cacheMessages1.size())

        then: "CacheMessages are delivered until all for a CacheChangeSet have been output"
        c.cacheChangeSetSubscription.awaitRequests()
        with(c.cacheMessageSubscriber) {
            awaitObjects()
            receivedObjects == cacheMessages1
            !hasCompleted
            !hasError
        }

        when: "Source of CacheChangeSets fails"
        c.cacheMessageSubscriber.expectError()
        c.cacheChangeSetSubscriber.onError(new RuntimeException("CacheChangeSets source has failed"))

        then: "CacheMessage subscription is failed"
        with(c.cacheMessageSubscriber) {
            awaitError()
            !hasCompleted
            hasError
            receivedError.message == "CacheChangeSets source has failed"
        }

        when: 
        c = new Components()
        c.cacheChangeSetSubscriber.onSubscribe(c.cacheChangeSetSubscription)

        then:
        c.cacheMessageSubscriber.awaitSubscribed()

        when: "CacheMessage subscriber requests CacheMessages but source of CacheChangeSets fails part-way"
        c.cacheChangeSetSubscription.expectRequests(2)
        c.cacheChangeSetSubscription.outputOnRequest { int request ->
            if(request < 1) {
                c.cacheChangeSetSubscriber.onNext(changeSet2)
            } else {
                c.cacheChangeSetSubscriber.onError(new RuntimeException("CacheChangeSets source has failed"))
            }
        }
        c.cacheMessageSubscriber.subscription.request(cacheMessages2.size() + cacheMessages3.size())

        then: "CacheMessage subscription is failed"
        with(c.cacheMessageSubscriber) {
            awaitError()
            !hasCompleted
            hasError
            receivedError.message == "CacheChangeSets source has failed"
        }
    }

    def "CacheChangeSetOutputStream cancels CacheChangeSet subscription if CacheMessage subscription is cancelled"() {

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

        when: "Subscription is started with source of CacheChangeSets"
        c.cacheChangeSetSubscriber.onSubscribe(c.cacheChangeSetSubscription)

        then: "CacheMessage subscriber is started"
        c.cacheMessageSubscriber.awaitSubscribed()

        when: "CacheMessage subscriber requests CacheMessages, and then cancels"
        c.cacheChangeSetSubscription.expectRequests(2)
        c.cacheChangeSetSubscription.expectCancel()
        c.cacheChangeSetSubscription.outputOnRequest { int request ->
            if(request < 1) {
                c.cacheChangeSetSubscriber.onNext(changeSet1)
            }
            else if(request == 1) {
                c.cacheMessageSubscriber.subscription.cancel()
            } else {
                throw new IllegalStateException("Should not request more CacheChangeSets if demand for CacheMessages is fulfilled")   
            }
        }
        c.cacheMessageSubscriber.subscription.request(cacheMessages1.size() + cacheMessages2.size())

        then: "CacheChangeSet subscription is cancelled, and the CacheMessage subscriber completes"
        with(c.cacheChangeSetSubscription) {
            awaitCancel()
            cancelRequested
        }
        with(c.cacheMessageSubscriber) {
            awaitCompleted()
            hasCompleted
            !hasError
        }
    }
}
