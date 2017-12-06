// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import com.modelcoding.opensource.jsoncache.messages.CacheChangeSetFrameAssembler.Receiver
import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Shared
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.messages.TestSuite.*

class CacheChangeSetFrameAssemblerSpecification extends Specification {

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

    def "CacheChangeSetFrameAssembler cannot be connected more than once"() {
        
        setup:
        def cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        
        when:
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        
        then:
        thrown(IllegalStateException)
    }
    
    def "CacheChangeSetFrameAssembler cannot be used until connected"() {
        
        setup:
        def cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        
        when:
        cacheChangeSetFrameAssembler.onCacheMessage(m.getCacheRemove("id"))
        
        then:
        thrown(IllegalStateException)
    }
    
    def "CacheChangeSetFrameAssembler decodes CacheMessages from JSON as expected"() {
        
        setup:
        def cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        def cacheObject = m.getCacheObject("id", "type", someContent)
        def cacheRemove = m.getCacheRemove("id")
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
        def startOfCacheChangeSet = g.getStartOfCacheChangeSet(changeSet)
        def endOfCacheChangeSet = g.getEndOfCacheChangeSet(changeSet)
        
        when:
        def fromJson_cacheObject = cacheChangeSetFrameAssembler.onMessage(cacheObject.asJsonNode())
        
        then:
        fromJson_cacheObject == cacheObject
        
        when:
        def fromJson_cacheRemove = cacheChangeSetFrameAssembler.onMessage(cacheRemove.asJsonNode())
        
        then:
        fromJson_cacheRemove == cacheRemove
        
        when:
        def fromJson_startOfCacheChangeSet = cacheChangeSetFrameAssembler.onMessage(startOfCacheChangeSet.asJsonNode())
        
        then:
        fromJson_startOfCacheChangeSet == startOfCacheChangeSet
        
        when:
        def fromJson_endOfCacheChangeSet = cacheChangeSetFrameAssembler.onMessage(endOfCacheChangeSet.asJsonNode())
        
        then:
        fromJson_endOfCacheChangeSet == endOfCacheChangeSet
        
        when:
        def fromJson_cacheChangeSet = cacheChangeSetFrameAssembler.onMessage(changeSet.asJsonNode())
        
        then:
        fromJson_cacheChangeSet == changeSet
        (fromJson_cacheChangeSet as CacheChangeSet).id == changeSet.id
    }
    
    def "CacheChangeSetFrameAssembler throws exception if decoding CacheMessages from JSON fails"() {
        
        setup:
        def cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        
        when:
        cacheChangeSetFrameAssembler.onMessage(asJsonNode([]))
        
        then:
        thrown(IllegalArgumentException)
        
        when:
        cacheChangeSetFrameAssembler.onMessage(asJsonNode([
            "id": "id",
            "type": "type"
        ]))
        
        then:
        thrown(IllegalArgumentException)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "CacheChangeSetFrameAssembler assembles frames as expected"() {
        
        setup:
        def cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
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
        def cacheChangeSetFrame1 = g.getCacheChangeSetFrame(changeSet1)
        def changeSet2 = m.getCacheChangeSet(
            "id", 
            [
                m.getCacheObject("A1", "AType", asJsonNode([]))
            ] as Set, 
            [
                
            ] as Set, 
            true
        )
        def cacheChangeSetFrame2 = g.getCacheChangeSetFrame(changeSet2)
        def changeSet3 = m.getCacheChangeSet(
            "id", 
            [
            ] as Set, 
            [
                m.getCacheRemove("A3")
            ] as Set, 
            false
        )
        def cacheChangeSetFrame3 = g.getCacheChangeSetFrame(changeSet3)
        def changeSet4 = m.getCacheChangeSet(
            "id", 
            [
            ] as Set, 
            [
            ] as Set, 
            false
        )
        def cacheChangeSetFrame4 = g.getCacheChangeSetFrame(changeSet4)
        def receiver = Mock(Receiver)
        
        when:
        cacheChangeSetFrameAssembler.connect(receiver)
        
        then:
        notThrown(Throwable)
        
        when: "input sequence is not complete"
        cacheChangeSetFrameAssembler.onCacheMessage(cacheChangeSetFrame1.messages.head())
        
        then: "receiver does not receive a frame"
        0 * receiver.onCacheChangeSetFrame()
        
        when: "input sequence for a frame completes"
        cacheChangeSetFrame1.messages.tail().each { cacheChangeSetFrameAssembler.onCacheMessage(it) }
        
        then: "receiver receives a frame"
        1 * receiver.onCacheChangeSetFrame({ CacheChangeSetFrame frame ->
            
            frame.cacheChangeSet == changeSet1
            frame.cacheChangeSet.id == changeSet1.id
            messagesMatch(changeSet1, frame)
        }) 

        when:
        cacheChangeSetFrameAssembler.onCacheMessage(cacheChangeSetFrame2.messages.head())
        
        then:
        0 * receiver.onCacheChangeSetFrame()
        
        when:
        cacheChangeSetFrame2.messages.tail().each { cacheChangeSetFrameAssembler.onCacheMessage(it) }
        
        then:
        1 * receiver.onCacheChangeSetFrame({ CacheChangeSetFrame frame ->
            
            frame.cacheChangeSet == changeSet2
            frame.cacheChangeSet.id == changeSet2.id
            messagesMatch(changeSet2, frame)
        }) 

        when:
        cacheChangeSetFrameAssembler.onCacheMessage(cacheChangeSetFrame3.messages.head())
        
        then:
        0 * receiver.onCacheChangeSetFrame()
        
        when:
        cacheChangeSetFrame3.messages.tail().each { cacheChangeSetFrameAssembler.onCacheMessage(it) }
        
        then:
        1 * receiver.onCacheChangeSetFrame({ CacheChangeSetFrame frame ->
            
            frame.cacheChangeSet == changeSet3
            frame.cacheChangeSet.id == changeSet3.id
            messagesMatch(changeSet3, frame)
        }) 

        when:
        cacheChangeSetFrameAssembler.onCacheMessage(cacheChangeSetFrame4.messages.head())
        
        then:
        0 * receiver.onCacheChangeSetFrame()
        
        when:
        cacheChangeSetFrame4.messages.tail().each { cacheChangeSetFrameAssembler.onCacheMessage(it) }
        
        then:
        1 * receiver.onCacheChangeSetFrame({ CacheChangeSetFrame frame ->
            
            frame.cacheChangeSet == changeSet4
            frame.cacheChangeSet.id == changeSet4.id
            messagesMatch(changeSet4, frame)
        }) 
    }

    def "CacheChangeSetFrameAssembler throws exception if input sequence is not correct"() {
        
        setup:
        def cacheObject = m.getCacheObject("A1", "AType", asJsonNode([]))
        def cacheRemove = m.getCacheRemove("A3")
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
        def startOfCacheChangeSet = g.getStartOfCacheChangeSet(changeSet)
        def endOfCacheChangeSet = g.getEndOfCacheChangeSet(changeSet)
        def diffIdChangeSet = m.getCacheChangeSet(
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
        )
        def diffIdEndOfCacheChangeSet = g.getEndOfCacheChangeSet(diffIdChangeSet)
        
        when: "first message is not StartOfCacheChangeSet"
        def cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(endOfCacheChangeSet)
        
        then:
        thrown(IllegalArgumentException)
        
        when:
        cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(cacheRemove)
        
        then:
        thrown(IllegalArgumentException)
        
        when:
        cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(cacheObject)
        
        then:
        thrown(IllegalArgumentException)
        
        when:
        cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(changeSet)
        
        then:
        thrown(IllegalArgumentException)
        
        when: "StartOfCacheChangeSet is received twice, with no EndOfCacheChangeSet in between"
        cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(startOfCacheChangeSet)
        cacheChangeSetFrameAssembler.onCacheMessage(startOfCacheChangeSet)
        
        then:
        thrown(IllegalArgumentException)
        
        when: "EndOfCacheChangeSet is received twice, with no StartOfCacheChangeSet in between"
        cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(startOfCacheChangeSet)
        cacheChangeSetFrameAssembler.onCacheMessage(endOfCacheChangeSet)
        cacheChangeSetFrameAssembler.onCacheMessage(endOfCacheChangeSet)
        
        then:
        thrown(IllegalArgumentException)
        
        when: "EndOfCacheChangeSet with unexpected id is received"
        cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(startOfCacheChangeSet)
        cacheChangeSetFrameAssembler.onCacheMessage(diffIdEndOfCacheChangeSet)
        
        then:
        thrown(IllegalArgumentException)
        
        when: "Too few puts received"
        cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(startOfCacheChangeSet)
        cacheChangeSetFrameAssembler.onCacheMessage(cacheObject)
        cacheChangeSetFrameAssembler.onCacheMessage(cacheRemove)
        
        then:
        thrown(IllegalArgumentException)
        
        when: "Too few removes received"
        cacheChangeSetFrameAssembler = g.cacheChangeSetFrameAssembler
        cacheChangeSetFrameAssembler.connect(Mock(Receiver))
        cacheChangeSetFrameAssembler.onCacheMessage(startOfCacheChangeSet)
        changeSet.puts.each { cacheChangeSetFrameAssembler.onCacheMessage(it) }
        cacheChangeSetFrameAssembler.onCacheMessage(endOfCacheChangeSet)
        
        then:
        thrown(IllegalArgumentException)
    }
}
