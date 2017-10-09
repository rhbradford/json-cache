// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import spock.lang.Shared
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.JsonCacheTestSuite.*

class CacheChangeSetSpecification extends Specification {

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

    def "CacheChangeSet is created as expected"() {

        setup:
        def puts = [
            m.getCacheObject("Id1", "Type", someContent),
            m.getCacheObject("Id2", "Type", someOtherContent)
        ] as Set
        def removes = [
            m.getCacheRemove("Id3"),
            m.getCacheRemove("Id4", someContent)
        ] as Set

        when:
        def cacheChangeSet = m.getCacheChangeSet(puts, removes)

        then:
        cacheChangeSet.puts == puts
        cacheChangeSet.removes == removes
    }

    def "CacheChangeSet cannot be created from bad parameters"() {

        setup:
        def puts = [
            m.getCacheObject("Id1", "Type", someContent),
            m.getCacheObject("Id2", "Type", someOtherContent)
        ] as Set
        def removes = [
            m.getCacheRemove("Id3"),
            m.getCacheRemove("Id4", someContent)
        ] as Set

        when:
        m.getCacheChangeSet(null, removes)

        then:
        thrown(IllegalArgumentException)

        when:
        m.getCacheChangeSet(puts, null)

        then:
        thrown(IllegalArgumentException)
    }

    def "CacheChangeSet accessors do not expose CacheChangeSet to mutation"() {

        setup:
        def puts = [
            m.getCacheObject("Id1", "Type", someContent),
            m.getCacheObject("Id2", "Type", someOtherContent)
        ] as Set
        def removes = [
            m.getCacheRemove("Id3"),
            m.getCacheRemove("Id4", someContent)
        ] as Set
        def thePuts = new HashSet(puts)
        def theRemoves = new HashSet(removes)

        when:
        def cacheChangeSet = m.getCacheChangeSet(puts, removes)
        def gotPuts = cacheChangeSet.puts
        def gotRemoves = cacheChangeSet.removes
        try {
            gotPuts.iterator().remove()
        }
        catch(ignored) {
        }
        try {
            gotPuts << m.getCacheObject("Id5", "Type", someContent)
        }
        catch(ignored) {
        }
        try {
            gotRemoves.clear()
        }
        catch(ignored) {
        }
        
        then:
        cacheChangeSet.puts == thePuts
        cacheChangeSet.removes == theRemoves
    }
}
