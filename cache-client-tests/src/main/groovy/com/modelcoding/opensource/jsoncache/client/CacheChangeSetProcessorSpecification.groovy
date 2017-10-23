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
    
    def "CacheChangeSetProcessor does not interact with CacheImageSender on connect"() {
        
        setup:
        def cacheImageSender = Mock(CacheImageSender)
        
        when:
        def selectors = Mock(Publisher)
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(cacheImageSender)
        
        then:
        0 * cacheImageSender._
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetProcessor subscribes to just the cacheObjectSelectors when a subscription is made for output CacheChangeSets"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def cacheImageSender = Mock(CacheImageSender)
        def subscriber = Mock(Subscriber)
        
        when:
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(cacheImageSender)
        processor.subscribe(subscriber)
        
        then:
        selectors.awaitSubscription()
        0 * cacheImageSender.subscribe(_)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetProcessor only subscribes to the CacheImageSender when the first selector is received"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def cacheImageSender = new MockCacheImageSender()
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
        processor.connect(cacheImageSender)
        processor.subscribe(subscriber)
        
        then:
        selectors.awaitSubscription()
        
        when:
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then:
        cacheImageSender.awaitSubscription()
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetProcessor completes its subscriber if cacheObjectSelectors subscription completes without publishing any selectors"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def cacheImageSender = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def selectorsSubscription = Mock(Subscription) {
            1 * request(1) >> { 
                selectors.subscriber.onComplete()
            }
            _ * request(1)
        }
        
        when:
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(cacheImageSender)
        processor.subscribe(subscriber)
        
        then:
        selectors.awaitSubscription()
        
        when:
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then:
        subscriber.awaitComplete()
    }

    @IgnoreRest
    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetProcessor uses received selector to filter change sets"() {
        
        setup:
        def selectors = new MockSelectorsPublisher()
        def cacheImageSender = new MockCacheImageSender()
        def subscriber = new MockSubscriber()
        def aSelector = { CacheObject cacheObject -> cacheObject.id.startsWith("A") } as Predicate<CacheObject>
        def selectorsSubscription = Mock(Subscription) {
            1 * request(1) >> { 
                selectors.subscriber.onNext(aSelector)
            }
            _ * request(1)
        }
        def cacheImageSenderSubscription = Mock(Subscription) {
            2 * request(1) >>  { 
                cacheImageSender.subscriber.onNext(
                    m.getCacheImage(
                        [
                            m.getCacheObject("A1", "AType", asJsonNode([])),
                            m.getCacheObject("A2", "AType", asJsonNode([])),
                            m.getCacheObject("B1", "BType", asJsonNode([])),
                            m.getCacheObject("C1", "CType", asJsonNode([]))
                        ] as Set
                    )
                )
            } >> {
                cacheImageSender.subscriber.onNext(
                    m.getCacheChangeSet(
                        [
                            m.getCacheObject("A3", "AType", asJsonNode([])),
                            m.getCacheObject("B2", "BType", asJsonNode([])),
                            m.getCacheObject("C2", "CType", asJsonNode([]))
                        ] as Set,
                        [
                            m.getCacheRemove("A2")
                        ] as Set
                    )
                )
            }
            _ * request(1)
        }
        subscriber.expect(2)
        
        when:
        def processor = c.getCacheChangeSetProcessor(selectors)
        processor.connect(cacheImageSender)
        processor.subscribe(subscriber)
        
        then:
        selectors.awaitSubscription()
        
        when:
        selectors.subscriber.onSubscribe(selectorsSubscription)
        
        then:
        cacheImageSender.awaitSubscription()
        
        when:
        cacheImageSender.subscriber.onSubscribe(cacheImageSenderSubscription)
        
        then:
        with(subscriber) {
            await()
            !hasError
            !completed
            changeSets == [
                m.getCacheImage(
                    [
                        m.getCacheObject("A1", "AType", asJsonNode([])),
                        m.getCacheObject("A2", "AType", asJsonNode([]))
                    ] as Set
                ),
                m.getCacheChangeSet(
                    [
                        m.getCacheObject("A3", "AType", asJsonNode([]))
                    ] as Set,
                    [
                        m.getCacheRemove("A2")
                    ] as Set
                )
            ]
        }
    }
    
    private static class MockCacheImageSender implements CacheImageSender {

        private CountDownLatch subscriptionRequests = new CountDownLatch(1)
        private CountDownLatch sendImageRequests
        
        boolean awaitSubscription(long milliseconds = 1000) {
            subscriptionRequests.await(milliseconds, TimeUnit.MILLISECONDS)
        }

        void expectedImageSends(int numberOfSendImageRequests) {
            sendImageRequests = new CountDownLatch(numberOfSendImageRequests)
            imageSubscribers.clear()
        }
        
        boolean awaitImageSends(long milliseconds = 1000) {
            sendImageRequests.await(milliseconds, TimeUnit.MILLISECONDS)
        }
        
        def imageSubscribers = [] as Set
        
        @Override
        void sendImageToSubscriber(final Subscriber<? super CacheChangeSet> subscriber) {
            imageSubscribers << subscriber
            sendImageRequests.countDown()
        }

        Subscriber<? super CacheChangeSet> subscriber
        
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
        private volatile boolean cancelled
        private volatile Subscription subscription
        private CountDownLatch notifications
        
        private CacheChangeSet specificNotification
        private CountDownLatch specificNotifications

        final List<CacheChangeSet> changeSets = []
        boolean completed
        boolean hasError
        
        private final CountDownLatch completion = new CountDownLatch(1)

        void expect(int expectedNumberOfNotifications) {
            notifications = new CountDownLatch(expectedNumberOfNotifications)
            changeSets.clear()
            completed = false
            hasError = false
        }

        boolean await(long milliseconds = 1000) {
            notifications.await(milliseconds, TimeUnit.MILLISECONDS)
        }

        void expectChangeSet(CacheChangeSet changeSet) {
            specificNotification = changeSet
            specificNotifications = new CountDownLatch(1)
        }
  
        boolean awaitChangeSet(long milliseconds = 1000) {
            specificNotifications.await(milliseconds, TimeUnit.MILLISECONDS)
        }
        
        boolean awaitComplete(long milliseconds = 1000) {
            completion.await(milliseconds, TimeUnit.MILLISECONDS)
        }
        
        void cancel() {
            cancelled = true
            subscription?.cancel()
        }
        
        protected void makeRequest(Subscription s) {
            s.request(1)
        }
        
        @Override
        void onSubscribe(final Subscription s) {
            subscription = s
            // Ensures that MockSubscriber should always receive the initial change set.
            // The initial change set always contains a put for each object currently in the JsonCache.
            makeRequest(subscription) 
            if(cancelled)
                subscription.cancel()
        }

        @Override
        void onNext(final CacheChangeSet cacheChangeSet) {
            changeSets << cacheChangeSet
            notifications?.countDown()
            if(cacheChangeSet == specificNotification)
                specificNotifications?.countDown()
            if(!cancelled)
                makeRequest(subscription)
        }

        @Override
        void onError(final Throwable t) {
            hasError = true
            println t
            notifications?.countDown()
        }

        @Override
        void onComplete() {
            completed = true
            completion.countDown()
            notifications?.countDown()
        }
    }
}
