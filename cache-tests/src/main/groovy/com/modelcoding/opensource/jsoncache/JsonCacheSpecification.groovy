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
            await(1000)
            changeSets == [m.getCacheChangeSet(preContent, [] as Set)]
            !completed
            !hasError
        }
        
        when: "A subsequent change is made to the cache"
        subscriber.expect(1)
        jsonCache.applyChanges(cacheChangeCalculator)
        
        then: "the subscriber receives a change set with the changes made"
        with(subscriber) {
            await(1000)
            changeSets == [cacheChangeSet]
            !completed
            !hasError
        }
        
        when: "The subscription is cancelled"
        subscriber.expect(1)
        subscriber.cancel()
        
        then: "the subscriber is completed"
        with(subscriber) {
            await(1000)
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
            await(1000)
            changeSets == [m.getCacheChangeSet(postContent, [] as Set)]
            completed
            !hasError
        }
    }

    def "Multiple subscribers receive expected notifications from JsonCache"() {
        
        
    }
    
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
        subscriber.expect(2)
        jsonCache.subscribe(subscriber)
        jsonCache.applyChanges(cacheChangeCalculator)
        
        then: "the notification of the initial change set and subsequent change set overfills the buffer, and the subscriber is completed"
        with(subscriber) {
            await(1000)
            changeSets == []
            completed
            hasError
        }
        
        when: "A new subscription is made"
        subscriber = new MockSubscriber()
        subscriber.expect(2)
        jsonCache.subscribe(subscriber)
        subscriber.cancel()
        
        then: "the new subscriber receives change sets as expected"
        with(subscriber) {
            await(1000)
            changeSets == [m.getCacheChangeSet(postContent, [] as Set)]
            completed
            !hasError
        }
    }
    
    private static class MockSubscriber implements Subscriber<CacheChangeSet>
    {
        private volatile boolean cancelled
        private volatile Subscription subscription
        private CountDownLatch notifications

        final changeSets = []
        boolean completed
        boolean hasError

        void expect(int expectedNumberOfNotifications) {
            notifications = new CountDownLatch(expectedNumberOfNotifications)
            changeSets.clear()
            completed = false
            hasError = false
        }

        boolean await(long milliseconds) {
            notifications.await(milliseconds, TimeUnit.MILLISECONDS)
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
            subscription = s;
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
            notifications.countDown()
        }
    }
}
