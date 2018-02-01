// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages.testSupport

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import com.modelcoding.opensource.jsoncache.messages.CacheChangeSetInputStream
import org.reactivestreams.Publisher

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockInputStreamObserver implements CacheChangeSetInputStream.Observer {

    Publisher<CacheChangeSet> publisher
    private CountDownLatch publisherReceived = new CountDownLatch(0)

    void expectPublisher() {
        publisher = null
        publisherReceived = new CountDownLatch(1)
    }
    
    boolean awaitPublisher(long timeout = 1000) {
        publisherReceived.await(timeout, TimeUnit.MILLISECONDS)
    }

    @Override
    void onSubscribed(final Publisher<CacheChangeSet> changeSetPublisher) {
        publisher = changeSetPublisher
        publisherReceived.countDown()
    }
}
