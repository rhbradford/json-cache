// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache; 

import spock.lang.Specification

import static com.modelcoding.opensource.jsoncache.JsonCacheTestSuite.m

class CacheLocationSpecification extends Specification {

    def "CacheLocation is created as expected"() {
        
        setup:
        def aCacheObjectId = "Id"
        
        when:
        def cacheLocation = m.getCacheLocation(aCacheObjectId)
        
        then:
        cacheLocation.cacheObjectId == aCacheObjectId
    }
    
    def "CacheLocation cannot be created from bad parameters"() {
        
        when:
        m.getCacheLocation(null)
        
        then:
        thrown(Exception)
    }
}
