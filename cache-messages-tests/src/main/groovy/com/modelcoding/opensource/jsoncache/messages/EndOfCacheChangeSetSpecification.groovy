// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.modelcoding.opensource.jsoncache.CacheChangeSet
import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.messages.TestSuite.*

class EndOfCacheChangeSetSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup


    def "EndOfCacheChangeSet is created as expected"() {
        
        setup:
        def changeSet = m.getCacheChangeSet(
            "id", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([])),
                m.getCacheObject("A2", "AType", asJsonNode([])),
                m.getCacheObject("B1", "BType", asJsonNode([])),
                m.getCacheObject("C1", "CType", asJsonNode([]))
            ] as Set, 
            [
                m.getCacheRemove("A3")
            ] as Set, 
            false
        )
        def json = asJsonNode(
            [
                "frame": "end",
                "id": "id"
            ]
        )
        
        when:
        def endOfCacheChangeSet = g.getEndOfCacheChangeSet(changeSet)
        
        then:
        endOfCacheChangeSet.id == "id"
        
        when:
        endOfCacheChangeSet = g.getEndOfCacheChangeSet(json)
        
        then:
        endOfCacheChangeSet.id == "id"
    }
    
    def "EndOfCacheChangeSet cannot be created from bad parameters"() {
        
        when:
        g.getEndOfCacheChangeSet(null as CacheChangeSet)
        
        then:
        thrown(NullPointerException)
        
        when:
        g.getEndOfCacheChangeSet(null as JsonNode)
        
        then:
        thrown(NullPointerException)
        
        when:
        g.getEndOfCacheChangeSet(JsonNodeFactory.instance.objectNode())

        then:
        thrown(IllegalArgumentException)
    }    

    def "Equal EndOfCacheChangeSet are equal"() {

        expect:
        g.getEndOfCacheChangeSet(c) == g.getEndOfCacheChangeSet(c)
        g.getEndOfCacheChangeSet(c).hashCode() == g.getEndOfCacheChangeSet(c).hashCode()

        where:
        c << [
            m.getCacheChangeSet(
                "id", 
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([])),
                    m.getCacheObject("B1", "BType", asJsonNode([])),
                    m.getCacheObject("C1", "CType", asJsonNode([]))
                ] as Set, 
                [
                    m.getCacheRemove("A3")
                ] as Set, 
                false
            )            
        ]
    }

    def "Equality must not rely on a specific implementation"() {

        setup:
        def c = 
            m.getCacheChangeSet(
                "id", 
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([])),
                    m.getCacheObject("B1", "BType", asJsonNode([])),
                    m.getCacheObject("C1", "CType", asJsonNode([]))
                ] as Set, 
                [
                    m.getCacheRemove("A3")
                ] as Set, 
                false
            )            
        
        when:
        def sccs1 = g.getEndOfCacheChangeSet(c)
        def sccs2 = new EndOfCacheChangeSet() {

            @Override
            String getId() {
                c.id
            }

            @Override
            ObjectNode asJsonNode() {
                null
            }
        }
        
        then:
        sccs1 == sccs2
    }

    def "Unequal EndOfCacheChangeSets are not equal"() {

        expect:
        g.getEndOfCacheChangeSet(a) != g.getEndOfCacheChangeSet(b)

        where:
        a << [
            m.getCacheChangeSet(
                "id", 
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([])),
                    m.getCacheObject("B1", "BType", asJsonNode([])),
                    m.getCacheObject("C1", "CType", asJsonNode([]))
                ] as Set, 
                [
                    m.getCacheRemove("A3")
                ] as Set, 
                false
            )            
        ]
        b << [
            m.getCacheChangeSet(
                "diff_id", 
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([])),
                    m.getCacheObject("B1", "BType", asJsonNode([])),
                    m.getCacheObject("C1", "CType", asJsonNode([]))
                ] as Set, 
                [
                    m.getCacheRemove("A3")
                ] as Set, 
                true
            )            
        ]
    }
}
