// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import spock.lang.Shared
import spock.lang.Specification

import static TestSuite.*

class CacheObjectSpecification extends Specification {

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

        when:
        def cacheObject = m.getCacheObject(anId, aType, someContent)

        then:
        cacheObject.id == anId
        cacheObject.type == aType
        cacheObject.content == someContent
        cacheObject.content == sameContent
    }

    def "CacheObject cannot be created from bad parameters"() {

        setup:
        def anId = "Id"
        def aType = "Type"

        when:
        m.getCacheObject(anId, aType, null)

        then:
        thrown(IllegalArgumentException)

        when:
        m.getCacheObject(anId, null, someContent)

        then:
        thrown(IllegalArgumentException)

        when:
        m.getCacheObject(null, aType, someContent)

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

    def "Unequal CacheObjects are not equal"() {

        expect:
        a != b

        where:
        a                                           | b
        m.getCacheObject("id", "type", someContent) | m.getCacheObject("otherId", "type", sameContent)
    }
}
