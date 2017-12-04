// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.messages.TestSuite.*

class CacheChangeSetFrameSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup


    def "CacheChangeSetFrame is created as expected"() {
        
        setup:
        def changeSet1 = m.getCacheChangeSet(
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
        def changeSet2 = m.getCacheChangeSet(
            "id", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([]))
            ] as Set, 
            [
                
            ] as Set, 
            true
        )
        
        when:
        def cacheChangeSetFrame = g.getCacheChangeSetFrame(changeSet1)
        
        then:
        cacheChangeSetFrame.cacheChangeSet.is(changeSet1)
        cacheChangeSetFrame.messages == [
            g.getStartOfCacheChangeSet(changeSet1),
            m.getCacheObject("A1", "AType", asJsonNode([])),
            m.getCacheObject("A2", "AType", asJsonNode([])),
            m.getCacheObject("B1", "BType", asJsonNode([])),
            m.getCacheObject("C1", "CType", asJsonNode([])),
            m.getCacheRemove("A3"),
            g.getEndOfCacheChangeSet(changeSet1)
        ]
        
        when:
        cacheChangeSetFrame = g.getCacheChangeSetFrame(changeSet2)
        
        then:
        cacheChangeSetFrame.cacheChangeSet.is(changeSet2)
        cacheChangeSetFrame.messages == [
            g.getStartOfCacheChangeSet(changeSet2),
            m.getCacheObject("A1", "AType", asJsonNode([])),
            g.getEndOfCacheChangeSet(changeSet2)
        ]
    }
    
    def "CacheChangeSetFrame cannot be created from bad parameters"() {
        
        when:
        g.getCacheChangeSetFrame(null)
        
        then:
        thrown(NullPointerException)
    }    
}
