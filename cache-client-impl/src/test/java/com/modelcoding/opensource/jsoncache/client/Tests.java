// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.modelcoding.opensource.jsoncache.ScalaJsonCacheModule;
import org.junit.BeforeClass;
import org.junit.rules.ExternalResource;

public class Tests extends TestSuite {

    private static class Setup extends ExternalResource {

        private ActorSystem system;
        
        @Override
        protected void before() throws Throwable {

            system = ActorSystem.create("TestActorSystem");
            
            m = new ScalaJsonCacheModule(system);
            
            c = new ScalaJsonCacheClientModule(m, system);
        }

        @Override
        protected void after() {

            TestKit.shutdownActorSystem(system);
            
            system = null;
        }
    }

    @BeforeClass
    public static void setup() {

        perTestMethodSetup = new Setup();
    }
}
