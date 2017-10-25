// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client.testsupport

import org.reactivestreams.Subscription

class MockSubscription implements Subscription {

    private final long timeout

    MockSubscription(long timeout = 1000) {
        this.timeout = timeout
    }

    private final Object requestMonitor = new Object()
    private boolean requested

    boolean outputOnRequest(Closure outputCode) {
        synchronized(requestMonitor) {
            if(!requested) {
                requestMonitor.wait(timeout)
                if(!requested) return false
            }
            requested = false
        }
        outputCode()
        return true
    }

    @Override
    void request(final long n) {
        synchronized(requestMonitor) {
            requested = true
            requestMonitor.notify()
        }
    }

    private final Object cancelRequestMonitor = new Object()
    private boolean cancelRequested

    boolean cancelOnRequest(Closure cancelCode) {
        synchronized(cancelRequestMonitor) {
            if(!cancelRequested) {
                cancelRequestMonitor.wait(timeout)
                if(!cancelRequested) return false
            }
            cancelRequested = false
        }
        cancelCode()
        return true
    }

    @Override
    void cancel() {
        synchronized(cancelRequestMonitor) {
            cancelRequested = true
            cancelRequestMonitor.notify()
        }
    }
}
