// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static com.modelcoding.opensource.jsoncache.TestSuite.*

class JsonCacheSpecification extends Specification {

    @Shared
        content =
            [
                aThing      : "stuff",
                anotherThing: 12
            ]
    @Shared
        someContent = asJsonNode(content)
    @Shared
        otherContent =
            [
                aThing      : "otherStuff",
                anotherThing: 12
            ]
    @Shared
        someOtherContent = asJsonNode(otherContent)

    def "JsonCache is created as expected"() {

        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def object2 =
            m.getCacheObject("Id2", "Type", someOtherContent)
        def content = [object1, object2] as Set
        def cache = m.getCache(content)

        when:
        def jsonCache = m.getJsonCache("id", 12, cache)

        then:
        jsonCache.id == "id"
        jsonCache.subscriberBacklogLimit == 12
    }

    def "JsonCache cannot be created from bad parameters"() {

        setup:
        def cache = m.getCache([] as Set)

        when: "cacheId is null"
        m.getJsonCache(null, 12, cache)

        then:
        thrown(IllegalArgumentException)

        when: "subscriberBacklogLimit is 0"
        m.getJsonCache("id", 0, cache)

        then:
        thrown(IllegalArgumentException)

        when: "subscriberBacklogLimit is negative"
        m.getJsonCache("id", -10, cache)

        then:
        thrown(IllegalArgumentException)

        when: "cache is null"
        m.getJsonCache("id", 10, null)

        then:
        thrown(IllegalArgumentException)
    }
    
    @Ignore
    def "Single subscriber receives expected notifications from JsonCache"() {
        
        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def object2 =
            m.getCacheObject("Id2", "Type", someOtherContent)
        def object3 =
            m.getCacheObject("Id3", "Type", someContent)
        def object4 =
            m.getCacheObject("Id1", "Type", someOtherContent)
        def puts = [object3, object4] as Set
        def removes = [
            m.getCacheRemove("Id2"),
            m.getCacheRemove("NotInCache")
        ] as Set
        CacheChangeSet cacheChangeSet = m.getCacheChangeSet(puts, removes)
        CacheChangeCalculator cacheChangeCalculator = m.getCacheChangeCalculator(cacheChangeSet)
        def preContent = [object1, object2] as Set
        def cache = m.getCache(preContent)
        def postContent = [object3, object4] as Set
        def jsonCache = m.getJsonCache("id", 2, cache)
        def subscriber = new MockSubscriber()
        
        when: "A new subscription is made"
        subscriber.expect(1)
        jsonCache.subscribe(subscriber)
        
        then: "the subscriber first receives a change set with a put for every object in the cache at that point"
        with(subscriber) {
            await()
            changeSets == [m.getCacheChangeSet(preContent, [] as Set)]
            !completed
            !hasError
        }
        
        when: "A subsequent change is made to the cache"
        subscriber.expect(1)
        jsonCache.applyChanges(cacheChangeCalculator)
        
        then: "the subscriber receives a change set with the changes made"
        with(subscriber) {
            await()
            changeSets == [cacheChangeSet]
            !completed
            !hasError
        }
        
        when: "The subscription is cancelled"
        subscriber.expect(1)
        subscriber.cancel()
        
        then: "the subscriber is completed"
        with(subscriber) {
            await()
            changeSets == []
            completed
            !hasError
        }
        
        when: "A new subscription is made, and then cancelled - i.e. to access the cache contents"
        subscriber = new MockSubscriber()
        subscriber.expect(2)
        jsonCache.subscribe(subscriber)
        subscriber.cancel()
        
        then: "the subscriber receives a change set with the expected puts for every object in the post-change cache"
        with(subscriber) {
            await()
            changeSets == [m.getCacheChangeSet(postContent, [] as Set)]
            completed
            !hasError
        }
    }

    private CacheObject cacheObject(int i) {
        m.getCacheObject("Id$i", "Type", someContent)
    }
    
    private CacheChangeSet cacheChangeSet(int i) {
        def puts = [cacheObject(i)] as Set
        def removes = i > 0 ? [m.getCacheRemove("Id${i-1}")] as Set : [] as Set
        m.getCacheChangeSet(puts, removes)
    }
    
    private CacheChangeSet cacheContent(int i) {
        def puts = [cacheObject(i)] as Set
        m.getCacheChangeSet(puts, [] as Set)
    }
    
    @Ignore
    def "Multiple subscribers receive expected notifications from JsonCache"() {
        
        setup:
        def jsonCache = m.getJsonCache("id", 2, m.getCache([] as Set))
        def subscriber1 = new MockSubscriber()
        def subscriber2 = new MockSubscriber()
        def subscriber3 = new MockSubscriber()

        when:
        subscriber1.expect(1)
        jsonCache.subscribe(subscriber1)
        
        then:
        with(subscriber1) {
            await()
            changeSets == [m.getCacheChangeSet([] as Set, [] as Set)]
            !completed
            !hasError
        }

        when:
        subscriber1.expect(1)
        jsonCache.applyChanges(m.getCacheChangeCalculator(cacheChangeSet(0)))
        
        then:
        with(subscriber1) {
            await()
            changeSets == [cacheChangeSet(0)]
            !completed
            !hasError
        }
        
        when:
        subscriber2.expect(1)
        jsonCache.subscribe(subscriber2)
        
        then:
        with(subscriber2) {
            await()
            changeSets == [cacheContent(0)]
            !completed
            !hasError
        }
        with(subscriber1) {
            changeSets == [cacheChangeSet(0)]
            !completed
            !hasError
        }

        when:
        subscriber1.expect(1)
        subscriber2.expect(1)
        jsonCache.applyChanges(m.getCacheChangeCalculator(cacheChangeSet(1)))
        
        then:
        with(subscriber1) {
            await()
            changeSets == [cacheChangeSet(1)]
            !completed
            !hasError
        }
        with(subscriber2) {
            await()
            changeSets == [cacheChangeSet(1)]
            !completed
            !hasError
        }
        
        when:
        subscriber3.expect(1)
        jsonCache.subscribe(subscriber3)
        
        then:
        with(subscriber3) {
            await()
            changeSets == [cacheContent(1)]
            !completed
            !hasError
        }
        with(subscriber1) {
            changeSets == [cacheChangeSet(1)]
            !completed
            !hasError
        }
        with(subscriber2) {
            changeSets == [cacheChangeSet(1)]
            !completed
            !hasError
        }

        when:
        subscriber1.expect(1)
        subscriber2.expect(1)
        subscriber3.expect(1)
        jsonCache.applyChanges(m.getCacheChangeCalculator(cacheChangeSet(2)))
        
        then:
        with(subscriber1) {
            await()
            changeSets == [cacheChangeSet(2)]
            !completed
            !hasError
        }
        with(subscriber2) {
            await()
            changeSets == [cacheChangeSet(2)]
            !completed
            !hasError
        }
        with(subscriber3) {
            await()
            changeSets == [cacheChangeSet(2)]
            !completed
            !hasError
        }

        when:
        subscriber1.expect(1)
        subscriber2.expect(1)
        subscriber1.cancel()
        subscriber2.cancel()
        
        then:
        with(subscriber1) {
            await()
            changeSets == []
            completed
            !hasError
        }
        with(subscriber2) {
            await()
            changeSets == []
            completed
            !hasError
        }
        with(subscriber3) {
            changeSets == [cacheChangeSet(2)]
            !completed
            !hasError
        }
        
        when:
        subscriber3.expect(1)
        jsonCache.applyChanges(m.getCacheChangeCalculator(cacheChangeSet(3)))
        
        then:
        with(subscriber3) {
            await()
            changeSets == [cacheChangeSet(3)]
            !completed
            !hasError
        }
        with(subscriber1) {
            changeSets == []
            completed
            !hasError
        }
        with(subscriber2) {
            changeSets == []
            completed
            !hasError
        }
        
        when:
        subscriber3.expect(1)
        subscriber3.cancel()
        
        then:
        with(subscriber3) {
            await()
            changeSets == []
            completed
            !hasError
        }
        with(subscriber1) {
            changeSets == []
            completed
            !hasError
        }
        with(subscriber2) {
            changeSets == []
            completed
            !hasError
        }
    }
    
    @Ignore
    def "JsonCache terminates slow subscribers"() {
        
        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def object2 =
            m.getCacheObject("Id2", "Type", someOtherContent)
        def object3 =
            m.getCacheObject("Id3", "Type", someContent)
        def object4 =
            m.getCacheObject("Id1", "Type", someOtherContent)
        def puts = [object3, object4] as Set
        def removes = [
            m.getCacheRemove("Id2"),
            m.getCacheRemove("NotInCache")
        ] as Set
        CacheChangeSet cacheChangeSet = m.getCacheChangeSet(puts, removes)
        CacheChangeCalculator cacheChangeCalculator = m.getCacheChangeCalculator(cacheChangeSet)
        def preContent = [object1, object2] as Set
        def cache = m.getCache(preContent)
        def postContent = [object3, object4] as Set
        def jsonCache = m.getJsonCache("id", 1, cache)
        def subscriber = new MockSubscriber(){

            @Override
            protected void makeRequest(final Subscription s) {
                // Do nothing - publisher buffer will fill up and backlogLimit will be hit
            }
        }
        
        when: "A subscription is made, and a change made to the cache, but the subscriber is 'slow'" 
        subscriber.expect(1)
        jsonCache.subscribe(subscriber)
        jsonCache.applyChanges(cacheChangeCalculator)
        
        then: "the notification of the initial change set and subsequent change set overfills the buffer, and the subscriber gets an error"
        with(subscriber) {
            await()
            changeSets == []
            !completed
            hasError
        }
        
        when: "A new subscription is made"
        subscriber = new MockSubscriber()
        subscriber.expect(2)
        jsonCache.subscribe(subscriber)
        subscriber.cancel()
        
        then: "the new subscriber receives change sets as expected"
        with(subscriber) {
            await()
            changeSets == [m.getCacheChangeSet(postContent, [] as Set)]
            completed
            !hasError
        }
    }
    
    def "JsonCache can be used from multiple threads"() {

        setup:
        def thread1Changes = [0, 3, 7].collect { i -> cacheChangeSet(i) } 
        def thread2Changes = [1, 2, 8].collect { i -> cacheChangeSet(i) } 
        def thread3Changes = [4, 5, 6].collect { i -> cacheChangeSet(i) } 
        def changeSets = (0..8).collect { i -> cacheChangeSet(i) }
        def jsonCache = m.getJsonCache("id", 10, m.getCache([] as Set))
        def subscriber1 = new MockSubscriber()
        def subscriber2 = new MockSubscriber()
        def subscriber3 = new MockSubscriber()
        
        when:
        new Thread({
            subscriber1.expect(1)
            jsonCache.subscribe(subscriber1)
            subscriber1.await()
            thread1Changes.each { c -> 
                subscriber1.expectNext(1)
                jsonCache.applyChanges(m.getCacheChangeCalculator(c)) 
                subscriber1.await()
                Thread.yield()
            }
            subscriber1.cancel()
        } as Runnable, "Subscriber1").start()
        new Thread({
            subscriber2.expect(1)
            jsonCache.subscribe(subscriber2)
            subscriber2.await()
            thread2Changes.each { c -> 
                subscriber2.expectNext(1)
                jsonCache.applyChanges(m.getCacheChangeCalculator(c)) 
                subscriber2.await()
                Thread.yield()
            }
            subscriber2.cancel()
        } as Runnable, "Subscriber2").start()
        new Thread({
            subscriber3.expect(1)
            jsonCache.subscribe(subscriber3)
            subscriber3.await()
            thread3Changes.each { c -> 
                subscriber3.expectNext(1)
                jsonCache.applyChanges(m.getCacheChangeCalculator(c)) 
                subscriber3.await()
                Thread.yield()
            }
            subscriber3.cancel()
        } as Runnable, "Subscriber3").start()
        
        then:
        subscriber1.awaitComplete()
        subscriber2.awaitComplete()
        subscriber3.awaitComplete()
        subscriber1.completed
        subscriber2.completed
        subscriber3.completed
        !subscriber1.hasError
        !subscriber2.hasError
        !subscriber3.hasError
        subscriber1.changeSets.size() >= thread1Changes.size()+1
        subscriber2.changeSets.size() >= thread2Changes.size()+1
        subscriber3.changeSets.size() >= thread3Changes.size()+1
        subscriber1.changeSets.drop(1).every { cacheChangeSet -> changeSets.contains(cacheChangeSet) }
        subscriber2.changeSets.drop(1).every { cacheChangeSet -> changeSets.contains(cacheChangeSet) }
        subscriber3.changeSets.drop(1).every { cacheChangeSet -> changeSets.contains(cacheChangeSet) }
        
        when:
        def subscriber = new MockSubscriber()
        subscriber.expect(1)
        jsonCache.subscribe(subscriber)
        subscriber.cancel()
        
        then:
        with(subscriber) {
            await()
            completed
            !hasError
        }
        subscriber.changeSets.size() == 1
        subscriber.changeSets[0].puts.size() > 0
    }
    
    private static class MockSubscriber implements Subscriber<CacheChangeSet>
    {
        private volatile boolean cancelled
        private volatile Subscription subscription
        private CountDownLatch notifications

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

        void expectNext(int expectedNumberOfNotifications) {
            notifications = new CountDownLatch(expectedNumberOfNotifications)
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
            notifications.countDown()
            if(!cancelled)
                makeRequest(subscription)
        }

        @Override
        void onError(final Throwable t) {
            hasError = true
            notifications.countDown()
        }

        @Override
        void onComplete() {
            completed = true
            completion.countDown()
            notifications.countDown()
        }
    }
}
