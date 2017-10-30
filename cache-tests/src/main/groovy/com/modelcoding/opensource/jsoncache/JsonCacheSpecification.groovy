// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.modelcoding.opensource.jsoncache.testsupport.MockSubscriber
import com.modelcoding.opensource.jsoncache.testsupport.MockSubscription
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.reactivestreams.Subscription
import spock.lang.Shared
import spock.lang.Specification

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

    private static CacheChangeSet cacheImage(Set<CacheObject> content) {
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
        thrown(NullPointerException)

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
        thrown(NullPointerException)
    }

    def "JsonCache throws exception when methods called with bad parameters"() {

        setup:
        def cache = m.getCache([] as Set)
        def jsonCache = m.getJsonCache("id", 12, cache)

        when: 
        jsonCache.onNext(null)

        then:
        thrown(NullPointerException)

        when: 
        jsonCache.subscribe(null)

        then:
        thrown(NullPointerException)

        when: 
        jsonCache.sendImageToSubscriber(null)

        then:
        thrown(NullPointerException)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
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
        CacheFunction cacheChangeCalculator = m.getCacheChangeCalculator(cacheChangeSet)
        def preContent = [object1, object2] as Set
        def cache = m.getCache(preContent)
        def postContent = [object3, object4] as Set
        def jsonCache = m.getJsonCache("id", 2, cache)
        def subscriber = new MockSubscriber()
        def doNothingCacheFunction = Mock(CacheFunction)
        
        when: "A new subscription is made"
        subscriber.expectChangeSets(1)
        jsonCache.subscribe(subscriber)
        
        then: "the subscriber first receives a change set with a put for every object in the cache at that point"
        with(subscriber) {
            awaitSubscription()
            awaitChangeSets()
            changeSets == [cacheImage(preContent)]
            !hasCompleted
            !hasError
        }
        
        when: "A subsequent change is made to the cache, followed by a request for a cache image"
        subscriber.expectChangeSets(2)
        jsonCache.onNext(cacheChangeCalculator)
        jsonCache.sendImageToSubscriber(subscriber)
        
        then: "the subscriber receives a change set with the changes made, followed by a cache image"
        with(subscriber) {
            awaitChangeSets()
            changeSets == [cacheChangeSet, cacheImage(postContent)]
            !hasCompleted
            !hasError
        }
        
        when: "The subscription is cancelled"
        subscriber.cancel()
        
        then: "the subscriber is completed"
        with(subscriber) {
            awaitComplete()
            hasCompleted
            !hasError
        }
        
        when: "A new subscription is made to access the cache contents"
        subscriber = new MockSubscriber()
        jsonCache.subscribe(subscriber)
        
        then: "subscription cancelled"
        subscriber.awaitSubscription()
        subscriber.cancel()
        
        then: "the subscriber receives a change set with the expected puts for every object in the post-change cache"
        with(subscriber) {
            awaitComplete()
            changeSets == [cacheImage(postContent)]
            hasCompleted
            !hasError
        }
        
        when: "A subscription has been established, and requested a change set"
        subscriber = new MockSubscriber()
        subscriber.expectChangeSets(1)
        jsonCache.subscribe(subscriber)
        
        then: "when the subscriber has received the initial cache image: a cache function that makes no changes is entered and then a cache image requested on the same thread"
        subscriber.awaitSubscription()
        subscriber.awaitChangeSets()
        subscriber.changeSets == [cacheImage(postContent)]
        subscriber.expectChangeSets(1)
        jsonCache.onNext(doNothingCacheFunction)
        jsonCache.sendImageToSubscriber(subscriber)
        
        then: "the cache function is executed (which makes no changes), and only the requested cache image CacheChangeSet is output to the subscriber"
        subscriber.awaitChangeSets()
        subscriber.changeSets == [cacheImage(postContent)]
        1 * doNothingCacheFunction.execute(_) >> { Cache c -> 
            new CacheFunction.Result() {
        
                @Override
                Cache getCache() {
                    c
                }
        
                @Override
                CacheChangeSet getChangeSet() {
                    m.getCacheChangeSet([] as Set, [] as Set, false)
                }
            }
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
        cacheImage(puts)
    }
    
    def "Multiple subscribers receive expected notifications from JsonCache"() {
        
        setup:
        def jsonCache = m.getJsonCache("id", 2, m.getCache([] as Set))
        def subscriber1 = new MockSubscriber()
        def subscriber2 = new MockSubscriber()
        def subscriber3 = new MockSubscriber()

        when:
        subscriber1.expectChangeSets(1)
        jsonCache.subscribe(subscriber1)
        
        then:
        with(subscriber1) {
            awaitChangeSets()
            changeSets == [cacheImage([] as Set)]
            !hasCompleted
            !hasError
        }

        when:
        subscriber1.expectChangeSets(1)
        jsonCache.onNext(m.getCacheChangeCalculator(cacheChangeSet(0)))
        
        then:
        with(subscriber1) {
            awaitChangeSets()
            changeSets == [cacheChangeSet(0)]
            !hasCompleted
            !hasError
        }
        
        when:
        subscriber2.expectChangeSets(1)
        jsonCache.subscribe(subscriber2)
        
        then:
        with(subscriber2) {
            awaitChangeSets()
            changeSets == [cacheContent(0)]
            !hasCompleted
            !hasError
        }
        with(subscriber1) {
            changeSets == [cacheChangeSet(0)]
            !hasCompleted
            !hasError
        }

        when:
        subscriber1.expectChangeSets(1)
        subscriber2.expectChangeSets(1)
        jsonCache.onNext(m.getCacheChangeCalculator(cacheChangeSet(1)))
        
        then:
        with(subscriber1) {
            awaitChangeSets()
            changeSets == [cacheChangeSet(1)]
            !hasCompleted
            !hasError
        }
        with(subscriber2) {
            awaitChangeSets()
            changeSets == [cacheChangeSet(1)]
            !hasCompleted
            !hasError
        }
        
        when:
        subscriber3.expectChangeSets(1)
        jsonCache.subscribe(subscriber3)
        
        then:
        with(subscriber3) {
            awaitChangeSets()
            changeSets == [cacheContent(1)]
            !hasCompleted
            !hasError
        }
        with(subscriber1) {
            changeSets == [cacheChangeSet(1)]
            !hasCompleted
            !hasError
        }
        with(subscriber2) {
            changeSets == [cacheChangeSet(1)]
            !hasCompleted
            !hasError
        }

        when:
        subscriber1.expectChangeSets(1)
        subscriber2.expectChangeSets(1)
        subscriber3.expectChangeSets(1)
        jsonCache.onNext(m.getCacheChangeCalculator(cacheChangeSet(2)))
        
        then:
        with(subscriber1) {
            awaitChangeSets()
            changeSets == [cacheChangeSet(2)]
            !hasCompleted
            !hasError
        }
        with(subscriber2) {
            awaitChangeSets()
            changeSets == [cacheChangeSet(2)]
            !hasCompleted
            !hasError
        }
        with(subscriber3) {
            awaitChangeSets()
            changeSets == [cacheChangeSet(2)]
            !hasCompleted
            !hasError
        }

        when:
        subscriber1.changeSets.clear()
        subscriber2.changeSets.clear()
        subscriber1.cancel()
        subscriber2.cancel()
        
        then:
        with(subscriber1) {
            awaitComplete()
            changeSets == []
            hasCompleted
            !hasError
        }
        with(subscriber2) {
            awaitComplete()
            changeSets == []
            hasCompleted
            !hasError
        }
        with(subscriber3) {
            changeSets == [cacheChangeSet(2)]
            !hasCompleted
            !hasError
        }
        
        when:
        subscriber3.expectChangeSets(1)
        jsonCache.onNext(m.getCacheChangeCalculator(cacheChangeSet(3)))
        
        then:
        with(subscriber3) {
            awaitChangeSets()
            changeSets == [cacheChangeSet(3)]
            !hasCompleted
            !hasError
        }
        with(subscriber1) {
            changeSets == []
            hasCompleted
            !hasError
        }
        with(subscriber2) {
            changeSets == []
            hasCompleted
            !hasError
        }
        
        when:
        subscriber3.changeSets.clear()
        subscriber3.cancel()
        
        then:
        with(subscriber3) {
            awaitComplete()
            changeSets == []
            hasCompleted
            !hasError
        }
        with(subscriber1) {
            changeSets == []
            hasCompleted
            !hasError
        }
        with(subscriber2) {
            changeSets == []
            hasCompleted
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
        CacheFunction cacheChangeCalculator = m.getCacheChangeCalculator(cacheChangeSet)
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
        subscriber.expectChangeSets(1)
        jsonCache.subscribe(subscriber)
        jsonCache.onNext(cacheChangeCalculator)
        
        then: "the notification of the initial change set and subsequent change set overfills the buffer, and the subscriber gets an error"
        with(subscriber) {
            awaitError()
            changeSets == []
            !hasCompleted
            hasError
        }
        
        when: "New subscriptions are made"
        subscriber = new MockSubscriber()
        jsonCache.subscribe(subscriber)
        
        then: "cancelled"
        subscriber.awaitSubscription()
        subscriber.cancel()
        
        then: "new subscribers continue to receive change sets as expected"
        with(subscriber) {
            awaitComplete()
            changeSets == [cacheImage(postContent)]
            hasCompleted
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
        def numInsertsAndRemoves = 4 // Must be > 2
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
        def maxNumberOfChangeSetsForASubscriber = 1 + 1 + (threadChanges.size() * threadChanges[0].size())
        def jsonCache = m.getJsonCache("id", maxNumberOfChangeSetsForASubscriber, m.getCache([] as Set))
        
        def thread = { int num ->
            def index = num - 1
            def subscriber = subscribers[index]
            // Thread.yield() calls will hopefully shake the execution up between threads     
            new Thread(
                {
                    subscriber.expectChangeSet(threadChanges[index].last())
                    jsonCache.subscribe(subscriber)
                    threadChanges[index].eachWithIndex { c, i ->
                        // The specification for a JsonCache guarantees that these changes MUST be received by a
                        // subscriber already registered by this thread 
                        jsonCache.onNext(m.getCacheChangeCalculator(c))
                        if(Math.random() > 0.5)
                            Thread.yield()
                        if(i == 1)
                            jsonCache.sendImageToSubscriber(subscriber)
                    }
                    subscriber.awaitChangeSet()
                    subscriber.cancel()
                } as Runnable,
                "Subscriber$num"
            )
        }
        
        when:
        threadIndexes.each { thread(it).start() }
        
        then:
        subscribers.each { subscriber ->
            subscriber.awaitComplete()
            assert subscriber.hasCompleted
            assert !subscriber.hasError
            assert subscriber.changeSets.size() >= 1+1+numChangesAppliedPerSubscriber
            List<CacheChangeSet> receivedNonImageChangeSets = new ArrayList(subscriber.changeSets.tail()) // Removes initial cache image
            List<CacheChangeSet> receivedRequestedImageChangeSets = receivedNonImageChangeSets.findAll { it.cacheImage }
            receivedNonImageChangeSets.removeAll(receivedRequestedImageChangeSets)
            assert receivedRequestedImageChangeSets.size() >= 1
            assert receivedNonImageChangeSets.every { cacheChangeSet -> possibleOutputChangeSets.contains(cacheChangeSet) } 
        }
        
        when: "Final contents of JsonCache is observed by subscribing"
        def subscriber = new MockSubscriber()
        jsonCache.subscribe(subscriber)
        
        then: "cancelling"
        subscriber.awaitSubscription()
        subscriber.cancel()
        
        then: "JsonCache contains only 'last' objects - all other inserts and removes have cancelled out"
        with(subscriber) {
            awaitComplete()
            hasCompleted
            !hasError
        }
        subscriber.changeSets == [cacheImage(lastObjects) ]
    }
    
    def "JsonCache as a subscriber completes all its subscribers when it is completed"() {
        
        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def object2 =
            m.getCacheObject("Id2", "Type", someOtherContent)
        def puts = [object1, object2] as Set
        def removes = [] as Set
        CacheChangeSet cacheChangeSet = cacheChangeSet(puts, removes)
        CacheFunction cacheChangeCalculator = m.getCacheChangeCalculator(cacheChangeSet)
        def cache = m.getCache([] as Set)
        def jsonCache = m.getJsonCache("id", 2, cache)
        def subscriber1 = new MockSubscriber()
        def subscriber2 = new MockSubscriber()
        def subscription = new MockSubscription()
        
        when:
        subscriber1.expectChangeSets(1)
        subscriber2.expectChangeSets(1)
        jsonCache.subscribe(subscriber1)
        jsonCache.subscribe(subscriber2)
        
        then:
        subscriber1.awaitSubscription()
        subscriber2.awaitSubscription()
        subscriber1.awaitChangeSets()
        subscriber2.awaitChangeSets()
        !subscriber1.hasError
        !subscriber2.hasError
        !subscriber1.hasCompleted
        !subscriber2.hasCompleted
        subscriber1.changeSets == [ m.getCacheChangeSet([] as Set, [] as Set, true) ]
        subscriber2.changeSets == [ m.getCacheChangeSet([] as Set, [] as Set, true) ]
        
        when:
        jsonCache.onSubscribe(null)
        
        then:
        thrown(NullPointerException)
        
        when:
        subscriber1.expectChangeSets(1)
        subscriber2.expectChangeSets(1)
        subscription.expectRequest()
        jsonCache.onSubscribe(subscription)
        
        then:
        subscription.awaitRequest()
        subscription.expectRequest()
        jsonCache.onNext(cacheChangeCalculator)
        
        then:
        subscription.expectRequest()
        subscriber1.awaitChangeSets()
        subscriber2.awaitChangeSets()
        !subscriber1.hasError
        !subscriber2.hasError
        !subscriber1.hasCompleted
        !subscriber2.hasCompleted
        subscriber1.changeSets == [ m.getCacheChangeSet(puts, removes, false) ]
        subscriber2.changeSets == [ m.getCacheChangeSet(puts, removes, false) ]
        
        when:
        jsonCache.onComplete()
        
        then:
        subscriber1.awaitComplete()
        subscriber2.awaitComplete()
        !subscriber1.hasError
        !subscriber2.hasError
        subscriber1.hasCompleted
        subscriber2.hasCompleted
    }

    def "JsonCache as a subscriber fails all its subscribers when it is failed"() {
        
        setup:
        def cache = m.getCache([] as Set)
        def jsonCache = m.getJsonCache("id", 2, cache)
        def subscriber1 = new MockSubscriber()
        def subscriber2 = new MockSubscriber()
        def subscription = new MockSubscription()
        def error = new RuntimeException("Error with input of CacheChangeCalculators")
        
        when:
        subscriber1.expectChangeSets(1)
        subscriber2.expectChangeSets(1)
        jsonCache.subscribe(subscriber1)
        jsonCache.subscribe(subscriber2)
        
        then:
        subscriber1.awaitSubscription()
        subscriber2.awaitSubscription()
        subscriber1.awaitChangeSets()
        subscriber2.awaitChangeSets()
        !subscriber1.hasError
        !subscriber2.hasError
        !subscriber1.hasCompleted
        !subscriber2.hasCompleted
        subscriber1.changeSets == [ m.getCacheChangeSet([] as Set, [] as Set, true) ]
        subscriber2.changeSets == [ m.getCacheChangeSet([] as Set, [] as Set, true) ]
        
        when:
        jsonCache.onError(null)
        
        then:
        thrown(NullPointerException)
        
        when:
        subscription.expectRequest()
        jsonCache.onSubscribe(subscription)
        jsonCache.onError(error)
        
        then:
        subscription.awaitRequest()
        subscriber1.awaitError()
        subscriber2.awaitError()
        subscriber1.hasError
        subscriber2.hasError
        !subscriber1.hasCompleted
        !subscriber2.hasCompleted
    }
}
