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

class StartOfCacheChangeSetSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup


    def "StartOfCacheChangeSet is created as expected"() {
        
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
            true
        )
        def json = asJsonNode(
            [
                "frame": "start",
                "id": "id",
                "isCacheImage": true,
                "numPuts" : 4,
                "numRemoves" : 1
            ]
        )
        
        when:
        def startOfCacheChangeSet = g.getStartOfCacheChangeSet(changeSet)
        
        then:
        startOfCacheChangeSet.id == "id"
        startOfCacheChangeSet.cacheImage
        startOfCacheChangeSet.numPuts == 4
        startOfCacheChangeSet.numRemoves == 1
        
        when:
        startOfCacheChangeSet = g.getStartOfCacheChangeSet(json)
        
        then:
        startOfCacheChangeSet.id == "id"
        startOfCacheChangeSet.cacheImage
        startOfCacheChangeSet.numPuts == 4
        startOfCacheChangeSet.numRemoves == 1
    }
    
    def "StartOfCacheChangeSet cannot be created from bad parameters"() {
        
        when:
        g.getStartOfCacheChangeSet(null as CacheChangeSet)
        
        then:
        thrown(NullPointerException)
        
        when:
        g.getStartOfCacheChangeSet(null as JsonNode)
        
        then:
        thrown(NullPointerException)
        
        when:
        g.getStartOfCacheChangeSet(JsonNodeFactory.instance.objectNode())

        then:
        thrown(IllegalArgumentException)
    }    

    def "Equal StartOfCacheChangeSets are equal"() {

        expect:
        g.getStartOfCacheChangeSet(c) == g.getStartOfCacheChangeSet(c)
        g.getStartOfCacheChangeSet(c).hashCode() == g.getStartOfCacheChangeSet(c).hashCode()

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
        def sccs1 = g.getStartOfCacheChangeSet(c)
        def sccs2 = new StartOfCacheChangeSet() {

            @Override
            int getNumPuts() {
                c.puts.size()
            }

            @Override
            int getNumRemoves() {
                c.removes.size()
            }

            @Override
            boolean isCacheImage() {
                c.cacheImage
            }

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

    def "Unequal StartOfCacheChangeSets are not equal"() {

        expect:
        g.getStartOfCacheChangeSet(a) != g.getStartOfCacheChangeSet(b)

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
            ),            
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
            ),            
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
            ),            
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
                true
            ),            
            m.getCacheChangeSet(
                "id", 
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([]))
                ] as Set, 
                [
                    m.getCacheRemove("A3")
                ] as Set, 
                false
            ),            
            m.getCacheChangeSet(
                "id", 
                [
                    m.getCacheObject("A1", "AType", asJsonNode([])),
                    m.getCacheObject("A2", "AType", asJsonNode([])),
                    m.getCacheObject("B1", "BType", asJsonNode([])),
                    m.getCacheObject("C1", "CType", asJsonNode([]))
                ] as Set, 
                [
                ] as Set, 
                false
            ),            
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
                false
            ),            
        ]
    }
}
