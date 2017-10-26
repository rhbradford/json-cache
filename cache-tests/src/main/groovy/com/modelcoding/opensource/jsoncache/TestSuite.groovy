// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Defines an entry point for testing a jsoncache implementation.
 * <p>
 * <b>Be aware:</b> the testing is <em>not</em> designed to run tests/test methods in parallel!
 * <p>
 * To test an implementation, subclass this TestSuite as follows:
 * <pre>
 * public class Tests extends TestSuite {
 *
 *     private static class Setup extends ExternalResource {
 * 
 *         {@literal @}Override
 *         protected void before() throws Throwable {
 *
 *             // Create a new instance of your implementation here, and set 'm'.
 *             // This instance will be used by a single test method, and then discarded.
 *             // e.g. m = new MyJsonCacheImpl();
 *         }
 *
 *         {@literal @}Override
 *         protected void after() {
 *
 *             // Destroy the instance/resources
 *        }
 *    }
 *
 *    {@literal @}BeforeClass
 *    public static void setup() {
 * 
 *        perTestMethodSetup = new Setup();
 *    }
 * }
 * </pre>    
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(
    [
        CacheObjectSpecification.class,
        CacheRemoveSpecification.class,
        CacheChangeSetSpecification.class,
//        CacheSpecification.class,
//        CacheChangeCalculatorSpecification.class,
//        JsonCacheSpecification.class
    ]
)
class TestSuite {

    public static ExternalResource perTestMethodSetup
    public static JsonCacheModule m

    static JsonNode asJsonNode(def content) {
        new ObjectMapper().valueToTree(content)
    }
}
