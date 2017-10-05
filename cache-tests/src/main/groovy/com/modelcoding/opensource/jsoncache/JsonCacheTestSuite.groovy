// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite.class)
@Suite.SuiteClasses(
    [
        CacheLocationSpecification.class
    ]
)
class JsonCacheTestSuite {
    
    public static JsonCacheModule m
}
