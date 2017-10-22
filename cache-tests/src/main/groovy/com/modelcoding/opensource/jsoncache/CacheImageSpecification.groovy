// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Shared
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.TestSuite.*

class CacheImageSpecification extends Specification {

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
    
    
    def "CacheImage is created as expected"() {

        when:
        def cacheImage = m.getCacheImage(puts)

        then:
        cacheImage.puts == puts
        cacheImage.removes == [] as Set
    }

    def "CacheImage cannot be created from bad parameters"() {

        when:
        m.getCacheImage(null)

        then:
        thrown(NullPointerException)
    }
    
    def "Equal CacheImages are equal"() {

        expect:
        m.getCacheImage(puts) == m.getCacheImage(puts)
        m.getCacheImage(puts).hashCode() == m.getCacheImage(puts).hashCode()
    }
    
    def "Unequal CacheImages are not equal"() {
        
        expect:
        m.getCacheImage(puts) != m.getCacheImage([] as Set)
        m.getCacheImage(puts) != m.getCacheChangeSet(puts, [] as Set)
    }

    def "CacheImage accessors do not expose CacheImage to mutation"() {

        setup:
        def thePuts = new HashSet(puts)

        when:
        def cacheImage = m.getCacheImage(puts)
        def gotPuts = cacheImage.puts
        def gotRemoves = cacheImage.removes
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
            gotRemoves << m.getCacheRemove("Id5")
        }
        catch(ignored) {
        }
        thePuts << m.getCacheObject("Id5", "Type", someContent)
        
        then:
        cacheImage.puts == new HashSet(puts)
        cacheImage.removes == new HashSet([] as Set)
    }
}
