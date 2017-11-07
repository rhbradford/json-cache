// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.TestSuite.*

class CacheRemoveSpecification extends Specification {

    @Rule
    private ExternalResource setup = perTestMethodSetup
    
    def "CacheRemove is created as expected"() {

        setup:
        def anId = "Id"
        def json = asJsonNode(
            [
                "id": anId
            ]
        )

        when:
        def cacheRemove = m.getCacheRemove(anId)

        then:
        cacheRemove.id == anId
        cacheRemove.asJsonNode() == json

        when:
        cacheRemove = m.getCacheRemove(json)

        then:
        cacheRemove.id == anId
        cacheRemove.asJsonNode() == json
    }

    def "CacheRemove cannot be created from bad parameters"() {

        when:
        m.getCacheRemove((String)null)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheRemove((JsonNode)null)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheRemove(JsonNodeFactory.instance.objectNode())

        then:
        thrown(IllegalArgumentException)
    }

    def "Equal CacheRemoves are equal"() {

        expect:
        a == b
        a.hashCode() == b.hashCode()

        where:
        a                                           | b
        m.getCacheRemove("id") | m.getCacheRemove("id")
    }

    def "Equality must not rely on a specific implementation"() {

        expect:
        m.getCacheRemove("id") == new CacheRemove() {

            @Override
            String getId() {
                "id"
            }

            @Override
            ObjectNode asJsonNode() {
                null
            }
        }
    }
    
    def "Unequal CacheRemoves are not equal"() {

        expect:
        a != b

        where:
        a                                           | b
        m.getCacheRemove("id") | m.getCacheRemove("otherId")
    }
}
