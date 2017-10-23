// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode
import org.junit.Rule
import org.junit.rules.ExternalResource
import spock.lang.Shared
import spock.lang.Specification

import static TestSuite.*

class CacheRemoveSpecification extends Specification {

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
    @Shared
        someEmptyContent = asJsonNode([:])

    def "CacheRemove is created as expected"() {

        setup:
        def anId = "Id"

        when:
        def cacheRemove = m.getCacheRemove(anId, someContent)

        then:
        cacheRemove.id == anId
        cacheRemove.content == someContent
        cacheRemove.content == sameContent

        when:
        cacheRemove = m.getCacheRemove(anId)

        then:
        cacheRemove.id == anId
        cacheRemove.content == someEmptyContent
    }

    def "CacheRemove cannot be created from bad parameters"() {

        setup:
        def anId = "Id"

        when:
        m.getCacheRemove(anId, null)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheRemove(null, someContent)

        then:
        thrown(NullPointerException)

        when:
        m.getCacheRemove(null)

        then:
        thrown(NullPointerException)
    }

    def "Equal CacheRemoves are equal"() {

        expect:
        a == b
        a.hashCode() == b.hashCode()

        where:
        a                                           | b
        m.getCacheRemove("id", someContent) | m.getCacheRemove("id", sameContent)
        m.getCacheRemove("id", someContent) | m.getCacheRemove("id", someOtherContent)
    }

    def "Equality must not rely on a specific implementation"() {

        expect:
        m.getCacheRemove("id") == new CacheRemove() {

            @Override
            String getId() {
                "id"
            }

            @Override
            JsonNode getContent() {
                asJsonNode([])
            }
        }
    }
    
    def "Unequal CacheRemoves are not equal"() {

        expect:
        a != b

        where:
        a                                           | b
        m.getCacheRemove("id", someContent) | m.getCacheRemove("otherId", sameContent)
    }
}
