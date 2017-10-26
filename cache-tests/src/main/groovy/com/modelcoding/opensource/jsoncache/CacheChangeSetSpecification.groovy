// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
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
        def cacheChangeSet = m.getCacheChangeSet(puts, removes, true)
        def json = asJsonNode(
            [
                "isCacheImage": true,
                "puts" : puts.collect { it.asJsonNode() },
                "removes" : removes.collect { it.asJsonNode() }
            ]
        )

        then:
        cacheChangeSet.puts == puts
        cacheChangeSet.removes == removes
        cacheChangeSet.cacheImage
        cacheChangeSet.asJsonNode() == json
        
        when:
        cacheChangeSet = m.getCacheChangeSet(json, m)
        
        then:
        cacheChangeSet.puts == puts
        cacheChangeSet.removes == removes
        cacheChangeSet.cacheImage
        cacheChangeSet.asJsonNode() == json
    }

    def "CacheChangeSet cannot be created from bad parameters"() {

        when:
        m.getCacheChangeSet(null, removes, false)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheChangeSet(puts, null, false)

        then:
        thrown(NullPointerException)
        
        when:
        m.getCacheChangeSet(null, m)

        then:
        thrown(NullPointerException)
        
        when:
        m.getCacheChangeSet(JsonNodeFactory.instance.objectNode(), null)

        then:
        thrown(NullPointerException)
        
        when:
        m.getCacheChangeSet(JsonNodeFactory.instance.objectNode(), m)

        then:
        thrown(IllegalArgumentException)
        
    }
    
    def "Equal CacheChangeSets are equal"() {

        expect:
        m.getCacheChangeSet(p, r, ci) == m.getCacheChangeSet(p, r, ci)
        m.getCacheChangeSet(p, r, ci).hashCode() == m.getCacheChangeSet(p, r, ci).hashCode()

        where:
        p    | r       | ci
        puts | removes | false
        puts | removes | true
    }
    
    def "Equality must not rely on a specific implementation"() {
        
        expect:
        m.getCacheChangeSet(puts, removes, false) == new CacheChangeSet() {

            @Override
            Set<? extends CacheObject> getPuts() {
                CacheChangeSetSpecification.this.puts
            }

            @Override
            Set<? extends CacheRemove> getRemoves() {
                CacheChangeSetSpecification.this.removes
            }

            @Override
            boolean isCacheImage() {
                false
            }

            @Override
            ObjectNode asJsonNode() {
                null
            }
        }
    }
    
    def "Unequal CacheChangeSets are not equal"() {
        
        expect:
        m.getCacheChangeSet(p, r, ci) != m.getCacheChangeSet(p_, r_, ci_)

        where:
        p         | r       | ci    | p_   | r_        | ci_
        puts      | removes | false | puts | [] as Set | false
        [] as Set | removes | false | puts | removes   | false
        puts      | removes | false | puts | removes   | true
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
        !cacheChangeSet.cacheImage
    }
}
