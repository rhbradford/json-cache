// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Shared
import spock.lang.Specification

import static TestSuite.*

class CacheChangeSetSpecification extends Specification {

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
        def cacheChangeSet = m.getCacheChangeSet(puts, removes, false)

        then:
        cacheChangeSet.puts == puts
        cacheChangeSet.removes == removes
        !cacheChangeSet.cacheImage

        when:
        cacheChangeSet = m.getCacheChangeSet(puts, [] as Set, true)

        then:
        cacheChangeSet.puts == puts
        cacheChangeSet.removes == [] as Set
        cacheChangeSet.cacheImage
    }

    def "CacheChangeSet cannot be created from bad parameters"() {

        when:
        m.getCacheChangeSet(null, removes, false)

        then:
        thrown(IllegalArgumentException)

        when:
        m.getCacheChangeSet(puts, null, false)

        then:
        thrown(IllegalArgumentException)

        when: "Attempting to create a cache image change set that contains some removes"
        m.getCacheChangeSet(puts, removes, true)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }
    
    def "Equal CacheChangeSets are equal"() {

        expect:
        m.getCacheChangeSet(puts, removes, false) == m.getCacheChangeSet(puts, removes, false)
        m.getCacheChangeSet(puts, [] as Set, true) == m.getCacheChangeSet(puts, [] as Set, true)
        m.getCacheChangeSet(puts, removes, false).hashCode() == m.getCacheChangeSet(puts, removes, false).hashCode()
        m.getCacheChangeSet(puts, [] as Set, true).hashCode() == m.getCacheChangeSet(puts, [] as Set, true).hashCode()
    }
    
    def "Unequal CacheChangeSets are not equal"() {
        
        expect:
        m.getCacheChangeSet(puts, removes, false) != m.getCacheChangeSet(puts, [] as Set, false)
        m.getCacheChangeSet(puts, removes, false) != m.getCacheChangeSet([] as Set, removes, false)
        m.getCacheChangeSet(puts, [] as Set, false) != m.getCacheChangeSet(puts, [] as Set, true)
    }

    def "CacheChangeSet accessors do not expose CacheChangeSet to mutation"() {

        setup:
        def thePuts = new HashSet(puts)
        def theRemoves = new HashSet(removes)

        when:
        def cacheChangeSet = m.getCacheChangeSet(puts, removes, false)
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
        thePuts << m.getCacheObject("Id5", "Type", someContent)
        theRemoves.clear() 
        
        then:
        cacheChangeSet.puts == new HashSet(puts)
        cacheChangeSet.removes == new HashSet(removes)
    }
}
