// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.JsonCacheTestSuite.*

class PutObjectSpecification extends Specification {

    def "PutObject is created as expected"() {
        
        setup:
        def anId = "Id"
        def aType = "Type"
        def content = 
            [
                aThing: "stuff",
                anotherThing: 12
            ]
        def someContent = asJsonNode(content)
        def sameContent = asJsonNode(content)
        
        when:
        def cacheObject = m.getPutObject(anId, aType, someContent)
        
        then:
        cacheObject.id == anId
        cacheObject.type == aType
        cacheObject.content == someContent
        cacheObject.content == sameContent
    }
    
}
