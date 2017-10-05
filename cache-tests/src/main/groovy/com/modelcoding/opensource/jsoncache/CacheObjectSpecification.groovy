// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.JsonCacheTestSuite.*

class CacheObjectSpecification extends Specification {

    def "CacheObject is created as expected"() {
        
        setup:
        def aCacheObjectId = "Id"
        def aCacheObjectType = "Type"
        def content = 
            [
                aThing: "stuff",
                anotherThing: 12
            ]
        def someCacheObjectContent = asJsonNode(content)
        def sameCacheObjectContent = asJsonNode(content)
        
        when:
        def cacheObject = m.getCacheObject(aCacheObjectId, aCacheObjectType, someCacheObjectContent)
        
        then:
        cacheObject.cacheObjectId == aCacheObjectId
        cacheObject.cacheObjectType == aCacheObjectType
        cacheObject.cacheObjectContent == someCacheObjectContent
        cacheObject.cacheObjectContent == sameCacheObjectContent
    }
    
}
