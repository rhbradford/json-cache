// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite.class)
@Suite.SuiteClasses(
    [
        CacheObjectSpecification.class,
        CacheRemoveSpecification.class,
        CacheChangeSetSpecification.class,
        CacheSpecification.class,
        CacheChangeCalculatorSpecification.class,
        JsonCacheSpecification.class
    ]
)
class TestSuite {

    public static JsonCacheModule m

    static JsonNode asJsonNode(def content) {
        new ObjectMapper().valueToTree(content)
    }
}
