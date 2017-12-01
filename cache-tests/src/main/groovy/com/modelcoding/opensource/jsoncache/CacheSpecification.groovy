// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.modelcoding.opensource.jsoncache.Cache.PutAction
import com.modelcoding.opensource.jsoncache.Cache.RemoveAction
import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Shared
import spock.lang.Specification

import static TestSuite.*

class CacheSpecification extends Specification {

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
        m.getCacheChangeSet("id", content, [] as Set, true)
    }
    
    def "Cache is created as expected"() {

        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def object2 =
            m.getCacheObject("Id2", "Type", someOtherContent)
        def content = [object1, object2] as Set
        def image = cacheImage(content) 

        when:
        def cache = m.getCache(content)

        then:
        cache.image == image
        cache.containsCacheObject("Id1")
        cache.containsCacheObject("Id2")
        !cache.containsCacheObject("NotInCache")
        cache.getCacheObject("Id1") == object1
        cache.getCacheObject("Id2") == object2
    }

    def "Cache cannot be created from bad parameters"() {

        setup:

        when:
        m.getCache(null)

        then:
        thrown(NullPointerException)
    }

    def "Cache objects accessor does not expose Cache to mutation"() {

        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def object2 =
            m.getCacheObject("Id2", "Type", someOtherContent)
        def content = [object1] as Set
        def image = cacheImage(content) 

        when:
        def cache = m.getCache(content)
        def gotObjects = cache.image.puts
        try {
            gotObjects.iterator().remove()
        }
        catch(ignored) {
        }
        try {
            gotObjects << object2
        }
        catch(ignored) {
        }

        then:
        cache.containsCacheObject("Id1")
        !cache.containsCacheObject("Id2")
        cache.image == image
    }

    def "Cache throws exception if bad parameters passed into methods"() {

        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def content = [object1] as Set
        def cache = m.getCache(content)

        when:
        cache.containsCacheObject(null)

        then:
        thrown(NullPointerException)

        when:
        cache.getCacheObject(null)

        then:
        thrown(NullPointerException)

        when:
        cache.put(null)

        then:
        thrown(NullPointerException)

        when:
        cache.remove(null)

        then:
        thrown(NullPointerException)
    }

    def "Cache throws exception if asked to get CacheObject not in Cache"() {

        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def content = [object1] as Set

        when:
        def cache = m.getCache(content)
        cache.getCacheObject("Id2")

        then:
        thrown(IllegalArgumentException)
    }

    def "Returned Cache is a new instance with expected objects on processing puts"() {

        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def object1_changed =
            m.getCacheObject("Id1", "Type", someOtherContent)
        def object2 =
            m.getCacheObject("Id2", "Type", someOtherContent)
        def object3 =
            m.getCacheObject("Id3", "Type", someOtherContent)
        def content = [object1, object2] as Set
        def image = cacheImage(content) 
        def cache = m.getCache(content)

        when: "Cache processes put for a new object not already in cache"
        def result = cache.put(object3)

        then: "a new Cache is returned containing the old objects plus the new object, the old Cache remains unaffected"
        !result.is(cache)
        cache.image == image
        cache.containsCacheObject("Id1")
        cache.containsCacheObject("Id2")
        !cache.containsCacheObject("Id3")
        cache.getCacheObject("Id1") == object1
        cache.getCacheObject("Id2") == object2
        result.action == PutAction.ADDED
        result.cache.image == cacheImage([object1, object2, object3] as Set)
        result.cache.containsCacheObject("Id1")
        result.cache.containsCacheObject("Id2")
        result.cache.containsCacheObject("Id3")
        !cache.containsCacheObject("NotInCache")
        result.cache.getCacheObject("Id1") == object1
        result.cache.getCacheObject("Id2") == object2
        result.cache.getCacheObject("Id3") == object3

        when:
        cache.getCacheObject("Id3")

        then:
        thrown(IllegalArgumentException)

        when: "Cache processes put for a new object instance to replace an instance already in cache"
        result = cache.put(object1_changed)

        then: "a new Cache is returned containing the old objects, with the expected instance replaced by the new object, the old Cache remains unaffected"
        !result.is(cache)
        cache.image == image
        cache.containsCacheObject("Id1")
        cache.containsCacheObject("Id2")
        !cache.containsCacheObject("NotInCache")
        cache.getCacheObject("Id1") == object1
        cache.getCacheObject("Id2") == object2
        result.action == PutAction.REPLACED
        result.cache.image == cacheImage([object1_changed, object2] as Set)
        result.cache.containsCacheObject("Id1")
        result.cache.containsCacheObject("Id2")
        !cache.containsCacheObject("NotInCache")
        result.cache.getCacheObject("Id1") == object1_changed
        result.cache.getCacheObject("Id2") == object2

        when: "Empty Cache processes put for a new object"
        cache = m.getCache([] as Set)
        result = cache.put(object1)

        then: "a new Cache is returned containing the new object, the old Cache remains unaffected"
        !result.is(cache)
        cache.image == cacheImage([] as Set)
        !cache.containsCacheObject("Id1")
        result.cache.image == cacheImage([object1] as Set)
        result.cache.containsCacheObject("Id1")
        !cache.containsCacheObject("NotInCache")
        result.cache.getCacheObject("Id1") == object1

        when:
        cache.getCacheObject("Id1")

        then:
        thrown(IllegalArgumentException)
    }

    def "Returned Cache is a new instance with expected objects on processing removes"() {

        setup:
        def object1 =
            m.getCacheObject("Id1", "Type", someContent)
        def object2 =
            m.getCacheObject("Id2", "Type", someOtherContent)
        def content = [object1, object2] as Set
        def image = cacheImage(content) 
        def cache = m.getCache(content)

        when: "Cache processes remove of an instance in the Cache"
        def result = cache.remove(m.getCacheRemove("Id2"))

        then: "a new Cache is returned, with the old objects minus the removed instance, the old Cache remains unaffected"
        !result.is(cache)
        cache.image == image
        cache.containsCacheObject("Id1")
        cache.containsCacheObject("Id2")
        !cache.containsCacheObject("NotInCache")
        cache.getCacheObject("Id1") == object1
        cache.getCacheObject("Id2") == object2
        result.action == RemoveAction.REMOVED
        result.cache.image == cacheImage([object1] as Set)
        result.cache.containsCacheObject("Id1")
        !result.cache.containsCacheObject("NotInCache")
        result.cache.getCacheObject("Id1") == object1

        when:
        result.cache.getCacheObject("Id2")

        then:
        thrown(IllegalArgumentException)

        when: "Cache processes remove of an instance not in the Cache"
        result = cache.remove(m.getCacheRemove("NotInCache"))

        then: "the same Cache is returned as is"
        result.cache.is(cache)
        cache.image == image
        cache.containsCacheObject("Id1")
        cache.containsCacheObject("Id2")
        !cache.containsCacheObject("NotInCache")
        cache.getCacheObject("Id1") == object1
        cache.getCacheObject("Id2") == object2
        result.action == RemoveAction.NO_CHANGE
    }
}
