// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Shared
import spock.lang.Specification

import static TestSuite.*

class CacheObjectSpecification extends Specification {

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
        sameContent = asJsonNode(content)
    @Shared
        otherContent =
            [
                aThing      : "otherStuff",
                anotherThing: 12
            ]
    @Shared
        someOtherContent = asJsonNode(otherContent)

    def "CacheObject is created as expected"() {

        setup:
        def anId = "Id"
        def aType = "Type"
        def json = asJsonNode(
            [
                "id" : anId,
                "type" : aType,
                "content" : someContent
            ]
        )

        when:
        def cacheObject = m.getCacheObject(anId, aType, someContent)

        then:
        cacheObject.id == anId
        cacheObject.type == aType
        cacheObject.content == someContent
        cacheObject.content == sameContent
        cacheObject.asJsonNode() == json
        
        when:
        cacheObject = m.getCacheObject(json)
        
        then:
        cacheObject.id == anId
        cacheObject.type == aType
        cacheObject.content == someContent
        cacheObject.asJsonNode() == json
    }

    def "CacheObject cannot be created from bad parameters"() {

        setup:
        def anId = "Id"
        def aType = "Type"

        when:
        m.getCacheObject(anId, aType, null)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheObject(anId, null, someContent)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheObject(null, aType, someContent)

        then:
        thrown(NullPointerException)
        
        when:
        m.getCacheObject(null)

        then:
        thrown(NullPointerException)
        
        when:
        m.getCacheObject(JsonNodeFactory.instance.objectNode())

        then:
        thrown(IllegalArgumentException)
    }

    def "Equal CacheObjects are equal"() {

        expect:
        a == b
        a.hashCode() == b.hashCode()

        where:
        a                                           | b
        m.getCacheObject("id", "type", someContent) | m.getCacheObject("id", "type", sameContent)
        m.getCacheObject("id", "type", someContent) | m.getCacheObject("id", "otherType", sameContent)
        m.getCacheObject("id", "type", someContent) | m.getCacheObject("id", "type", someOtherContent)
    }

    def "Equality must not rely on a specific implementation"() {

        expect:
        m.getCacheObject("id", "type", someContent) == new CacheObject() {

            @Override
            String getId() {
                "id"
            }

            @Override
            String getType() {
                "type"
            }

            @Override
            JsonNode getContent() {
                someContent
            }

            @Override
            CacheRemove asCacheRemove() {
                null
            }

            @Override
            ObjectNode asJsonNode() {
                null
            }
        }
    }
    
    def "Unequal CacheObjects are not equal"() {

        expect:
        a != b

        where:
        a                                           | b
        m.getCacheObject("id", "type", someContent) | m.getCacheObject("otherId", "type", sameContent)
    }
    
    def "CacheObject creates its CacheRemove as expected"() {
        
        setup:
        def anId = "Id"
        def aType = "Type"

        when:
        def cacheObject = m.getCacheObject(anId, aType, someContent)
        def cacheRemove = cacheObject.asCacheRemove()
        
        then:
        cacheRemove.id == anId
    }
}
