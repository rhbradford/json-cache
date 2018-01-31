// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages.testSupport

import com.modelcoding.opensource.jsoncache.CacheMessage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockCacheMessagesPublisher implements Publisher<CacheMessage> {

    Subscriber<? super CacheMessage> subscriber

    private final CountDownLatch subscriptionRequests = new CountDownLatch(1)

    boolean awaitSubscription(long milliseconds = 1000) {
        subscriptionRequests.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    @Override
    void subscribe(final Subscriber<? super CacheMessage> s) {
        subscriber = s
        subscriptionRequests.countDown()
    }
}

