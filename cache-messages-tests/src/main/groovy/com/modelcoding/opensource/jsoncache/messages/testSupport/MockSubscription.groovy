// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages.testSupport

import org.reactivestreams.Subscription

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockSubscription implements Subscription {

    int requestCount
    private Closure outputCode
    private CountDownLatch requestReceived = new CountDownLatch(0)

    void expectRequests(int numRequests) {
        requestCount = 0
        requestReceived = new CountDownLatch(numRequests)
    }
    
    void outputOnRequest(Closure outputCode) {
        this.outputCode = outputCode
    }

    boolean awaitRequests(long timeout = 1000) {
        requestReceived.await(timeout, TimeUnit.MILLISECONDS)
    }
    
    @Override
    void request(final long n) {
        n.times {
            outputCode?.call(requestCount)
            requestCount++
            requestReceived.countDown()
        }
    }

    private CountDownLatch cancelReceived = new CountDownLatch(0)
    boolean cancelRequested

    void expectCancel() {
        cancelRequested = false
        cancelReceived = new CountDownLatch(1)
    }
    
    boolean awaitCancel(long timeout = 1000) {
        cancelReceived.await(timeout, TimeUnit.MILLISECONDS)
    }

    @Override
    void cancel() {
        cancelRequested = true
        cancelReceived.countDown()
    }
}
