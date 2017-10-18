// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static com.modelcoding.opensource.jsoncache.TestSuite.*

class JsonCacheSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup
    
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

    private static CacheChangeSet cacheImageChangeSet(Set<CacheObject> content) {
        m.getCacheChangeSet(content, [] as Set, true)
    }
    
    private static CacheChangeSet cacheChangeSet(Set<CacheObject> puts, Set<CacheRemove> removes) {
        m.getCacheChangeSet(puts, removes, false)
    }
    
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
        CacheChangeSet cacheChangeSet = cacheChangeSet(puts, removes)
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
            changeSets == [cacheImageChangeSet(preContent)]
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
            changeSets == [cacheImageChangeSet(postContent)]
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
        cacheChangeSet(puts, removes)
    }
    
    private CacheChangeSet cacheContent(int i) {
        def puts = [cacheObject(i)] as Set
        cacheImageChangeSet(puts)
    }
    
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
            changeSets == [cacheImageChangeSet([] as Set)]
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
        CacheChangeSet cacheChangeSet = cacheChangeSet(puts, removes)
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
        
        when: "New subscriptions are made"
        subscriber = new MockSubscriber()
        subscriber.expect(2)
        jsonCache.subscribe(subscriber)
        subscriber.cancel()
        
        then: "new subscribers continue to receive change sets as expected"
        with(subscriber) {
            await()
            changeSets == [cacheImageChangeSet(postContent)]
            completed
            !hasError
        }
    }
    
    private static CacheChangeSet changeSet(Object... entries) {
        
        def puts = [] as Set
        def removes = [] as Set
        
        entries.each {
            (it instanceof CacheObject) ? puts << it : removes << it
        }
        
        cacheChangeSet(puts, removes)
    }
    
    def "JsonCache can be used from multiple threads"() {

        setup:
        def threadIndexes = (1..3)
        def numInsertsAndRemoves = 20
        // Create sequences of changes where all but the last change cancels out
        List<List<CacheChangeSet>> threadChanges = threadIndexes.collect { int threadNum ->
            def changeSets = []
            numInsertsAndRemoves.times { 
                def insertObject =
                    m.getCacheObject("Id$it", "Type", someContent)
                def removeObject = 
                    m.getCacheRemove("Id$it")
                changeSets << changeSet(insertObject)
                changeSets << changeSet(removeObject)
            }
            def insertLastObject = 
                m.getCacheObject("End$threadNum", "Type", someOtherContent)
            changeSets << changeSet(insertLastObject)
            changeSets
        }
        def lastObjects = threadChanges.inject([] as Set) { Set set, listOfChangeSets ->
            set.addAll(listOfChangeSets.last().puts)
            set
        }
        Set<CacheChangeSet> possibleOutputChangeSets = threadChanges.inject([] as Set) { Set set, changeListForThread -> 
            set.addAll(changeListForThread)
            set
        }
        def numChangesAppliedPerSubscriber = threadChanges[0].size()
        def subscribers = threadIndexes.collect { new MockSubscriber() }
        // Note that the publisher buffer to a subscriber must potentially hold all possible changes for a
        // subscriber to avoid 'slow' subscriber detection
        def maxNumberOfChangeSetsForASubscriber = 1 + (threadChanges.size() * threadChanges[0].size())
        def jsonCache = m.getJsonCache("id", maxNumberOfChangeSetsForASubscriber, m.getCache([] as Set))
        
        def thread = { int num ->
            def index = num - 1
            // Thread.yield() calls will hopefully shake the execution up between threads     
            new Thread(
                {
                    subscribers[index].expectChangeSet(threadChanges[index].last())
                    jsonCache.subscribe(subscribers[index])
                    threadChanges[index].each { c ->
                        // The specification for a JsonCache guarantees that these changes MUST be received by a
                        // subscriber already registered by this thread 
                        jsonCache.applyChanges(m.getCacheChangeCalculator(c))
                        if(Math.random() > 0.5)
                            Thread.yield()
                    }
                    subscribers[index].awaitChangeSet()
                    subscribers[index].cancel()
                } as Runnable,
                "Subscriber$num"
            )
        }
        
        when:
        threadIndexes.each { thread(it).start() }
        
        then:
        subscribers.each { subscriber ->
            subscriber.awaitComplete()
            assert subscriber.completed
            assert !subscriber.hasError
            assert subscriber.changeSets.size() >= 1+numChangesAppliedPerSubscriber
            assert subscriber.changeSets.drop(1).every { cacheChangeSet -> possibleOutputChangeSets.contains(cacheChangeSet) } 
        }
        
        when: "Final contents of JsonCache is observed"
        def subscriber = new MockSubscriber()
        jsonCache.subscribe(subscriber)
        subscriber.cancel()
        
        then: "JsonCache contains only 'last' objects - all other inserts and removes have cancelled out"
        with(subscriber) {
            awaitComplete()
            completed
            !hasError
        }
        subscriber.changeSets == [ cacheImageChangeSet(lastObjects) ]
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
