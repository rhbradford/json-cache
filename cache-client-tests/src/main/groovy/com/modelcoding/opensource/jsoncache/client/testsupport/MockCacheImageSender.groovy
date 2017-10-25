// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client.testsupport

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import com.modelcoding.opensource.jsoncache.CacheImageSender
import org.reactivestreams.Subscriber

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockCacheImageSender implements CacheImageSender {

    Subscriber<? super CacheChangeSet> subscriber
    Subscriber<? super CacheChangeSet> sendImageSubscriber
    
    private final CountDownLatch subscriptionRequests = new CountDownLatch(1)
    private CountDownLatch sendImageRequests
    
    boolean awaitSubscription(long milliseconds = 1000) {
        subscriptionRequests.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    void expectSendImageRequest() {
        sendImageSubscriber = null
        sendImageRequests = new CountDownLatch(1)
    }
    
    boolean awaitSendImageRequest(long milliseconds = 1000) {
        sendImageRequests.await(milliseconds, TimeUnit.MILLISECONDS)
    }
    
    @Override
    void sendImageToSubscriber(final Subscriber<? super CacheChangeSet> subscriber) {
        sendImageSubscriber = subscriber
        sendImageRequests.countDown()
    }

    @Override
    void subscribe(final Subscriber<? super CacheChangeSet> s) {
        subscriber = s
        subscriptionRequests.countDown()
    }
}
