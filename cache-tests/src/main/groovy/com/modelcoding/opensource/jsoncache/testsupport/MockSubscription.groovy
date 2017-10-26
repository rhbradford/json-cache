// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.testsupport

import org.reactivestreams.Subscription

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockSubscription implements Subscription {

    private CountDownLatch requested

    void expectRequest() {
        requested = new CountDownLatch(1)
    }

    boolean awaitRequest(long milliSeconds = 1000) {
        requested.await(milliSeconds, TimeUnit.MILLISECONDS)
    }

    @Override
    void request(final long n) {
        requested.countDown()
    }

//        private CountDownLatch cancelled
//    
//        void expectCancel() {
//            cancelled = new CountDownLatch(1)
//        }
//        
//        boolean awaitCancel(long milliSeconds = 1000) {
//            cancelled.await(milliSeconds, TimeUnit.MILLISECONDS)
//        }

    @Override
    void cancel() {
//            cancelled.countDown()
    }
}
