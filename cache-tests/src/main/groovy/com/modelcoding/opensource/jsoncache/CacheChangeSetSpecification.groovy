// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import spock.lang.Shared
import spock.lang.Specification

import static TestSuite.*

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
    @Shared
        puts = [
            m.getCacheObject("Id1", "Type", someContent),
            m.getCacheObject("Id2", "Type", someOtherContent)
        ] as Set
    @Shared
        removes = [
            m.getCacheRemove("Id3"),
            m.getCacheRemove("Id4", someContent)
        ] as Set
    
    
    def "CacheChangeSet is created as expected"() {

        when:
        def cacheChangeSet = m.getCacheChangeSet(puts, removes)

        then:
        cacheChangeSet.puts == puts
        cacheChangeSet.removes == removes
    }

    def "CacheChangeSet cannot be created from bad parameters"() {

        when:
        m.getCacheChangeSet(null, removes)

        then:
        thrown(IllegalArgumentException)

        when:
        m.getCacheChangeSet(puts, null)

        then:
        thrown(IllegalArgumentException)
    }
    
    def "Equal CacheChangeSets are equal"() {

        expect:
        m.getCacheChangeSet(puts, removes) == m.getCacheChangeSet(puts, removes)
        m.getCacheChangeSet(puts, removes).hashCode() == m.getCacheChangeSet(puts, removes).hashCode()
    }
    
    def "Unequal CacheChangeSets are not equal"() {
        
        expect:
        m.getCacheChangeSet(puts, removes) != m.getCacheChangeSet(puts, [] as Set)
        m.getCacheChangeSet(puts, removes) != m.getCacheChangeSet([] as Set, removes)
    }

    def "CacheChangeSet accessors do not expose CacheChangeSet to mutation"() {

        setup:
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