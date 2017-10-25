// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client.testsupport

import com.modelcoding.opensource.jsoncache.CacheObject
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Predicate

class MockSelectorsPublisher implements Publisher<Predicate<CacheObject>> {

    Subscriber<? super Predicate<CacheObject>> subscriber

    private final CountDownLatch subscriptionRequests = new CountDownLatch(1)

    boolean awaitSubscription(long milliseconds = 1000) {
        subscriptionRequests.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    @Override
    void subscribe(final Subscriber<? super Predicate<CacheObject>> s) {
        subscriber = s
        subscriptionRequests.countDown()
    }
}

