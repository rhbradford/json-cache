// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.messages.TestSuite.*

class CacheChangeSetFrameSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup

    static boolean messagesMatch(CacheChangeSet changeSet, CacheChangeSetFrame frame) {
        int numPuts = changeSet.puts.size()
        int numRemoves = changeSet.removes.size()
        int size = 1 + numPuts + numRemoves + 1
        
        frame.messages.size() == size &&
        frame.messages.head() == g.getStartOfCacheChangeSet(changeSet) &&
        frame.messages.subList(1,numPuts+1) as Set == changeSet.puts && 
        frame.messages.subList(numPuts+1,numPuts+1+numRemoves) as Set == changeSet.removes && 
        frame.messages.last() == g.getEndOfCacheChangeSet(changeSet)
    }

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
        messagesMatch(changeSet1, cacheChangeSetFrame)
        
        when:
        cacheChangeSetFrame = g.getCacheChangeSetFrame(changeSet2)
        
        then:
        cacheChangeSetFrame.cacheChangeSet.is(changeSet2)
        messagesMatch(changeSet2, cacheChangeSetFrame)
    }
    
    def "CacheChangeSetFrame cannot be created from bad parameters"() {
        
        when:
        g.getCacheChangeSetFrame(null)
        
        then:
        thrown(NullPointerException)
    }    
}
