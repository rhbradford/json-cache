// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Shared
import spock.lang.Specification

import static TestSuite.*

class CacheChangeCalculatorSpecification extends Specification {

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

    def "CacheChangeCalculator is created as expected"() {

        setup:
        def puts = [
            m.getCacheObject("Id1", "Type", someContent),
            m.getCacheObject("Id2", "Type", someOtherContent)
        ] as Set
        def removes = [
            m.getCacheRemove("Id3"),
            m.getCacheRemove("Id4", someContent)
        ] as Set
        CacheChangeSet cacheChangeSet = m.getCacheChangeSet(puts, removes, false)

        when:
        def cacheChangeCalculator = m.getCacheChangeCalculator(cacheChangeSet)

        then:
        cacheChangeCalculator.changeSet == cacheChangeSet
    }

    def "CacheChangeCalculator cannot be created from bad parameters"() {

        when:
        m.getCacheChangeCalculator(null)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheChangeCalculator(m.getCacheChangeSet([] as Set, [] as Set, true))

        then:
        thrown(IllegalArgumentException)
    }

    def "CacheChangeCalculator creates the expected results when applied to a Cache"() {

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
        CacheChangeSet cacheChangeSet = m.getCacheChangeSet(puts, removes, false)
        CacheChangeCalculator cacheChangeCalculator = m.getCacheChangeCalculator(cacheChangeSet)
        def preContent = [object1, object2] as Set
        def cache = m.getCache(preContent)
        def postContent = [object3, object4] as Set

        when:
        def results = cacheChangeCalculator.calculateChange(cache)

        then:
        !results.cache.is(cache)
        results.cache.getImage().puts == postContent
        results.changeSet == cacheChangeSet
    }
}
