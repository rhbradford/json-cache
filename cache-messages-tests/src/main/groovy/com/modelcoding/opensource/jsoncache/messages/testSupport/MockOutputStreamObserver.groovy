// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages.testSupport

import com.modelcoding.opensource.jsoncache.CacheMessage
import com.modelcoding.opensource.jsoncache.messages.CacheChangeSetOutputStream
import org.reactivestreams.Publisher

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockOutputStreamObserver implements CacheChangeSetOutputStream.Observer {

    Publisher<CacheMessage> publisher
    private CountDownLatch publisherReceived = new CountDownLatch(0)

    void expectPublisher() {
        publisher = null
        publisherReceived = new CountDownLatch(1)
    }
    
    boolean awaitPublisher(long timeout = 1000) {
        publisherReceived.await(timeout, TimeUnit.MILLISECONDS)
    }

    @Override
    void onSubscribed(final Publisher<CacheMessage> cacheMessagePublisher) {
        publisher = cacheMessagePublisher
        publisherReceived.countDown()
    }
}
