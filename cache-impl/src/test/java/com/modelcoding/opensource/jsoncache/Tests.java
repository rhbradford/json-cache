// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import org.junit.BeforeClass;

public class Tests extends JsonCacheTestSuite {

    @BeforeClass
    public static void setup() {
        
        m = ScalaJsonCacheModule$.MODULE$;
    }
}