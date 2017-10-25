// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.modelcoding.opensource.jsoncache.JsonCacheModule
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Defines an entry point for testing a jsoncache client implementation.
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
 *             // Create a new instance of a jsoncache implementation here, and set 'm'.
 *             // This instance will be used by a single test method, and then discarded.
 *             // e.g. m = new MyJsonCacheImpl();
 *             
 *             // Create a new instance of your jsoncache client implementation here, and set 'c'.
 *             // This instance will be used by a single test method, and then discarded.
 *             // e.g. c = new MyJsonCacheClientImpl();
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
        CacheChangeSetProcessorSpecification.class,
        JsonCacheClientSpecification.class
    ]
)
class TestSuite {

    public static ExternalResource perTestMethodSetup
    public static JsonCacheModule m
    public static JsonCacheClientModule c

    static JsonNode asJsonNode(def content) {
        new ObjectMapper().valueToTree(content)
    }
}
