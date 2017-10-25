// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client.testsupport

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockSubscriber implements Subscriber<CacheChangeSet> {

    Subscription subscription

    CacheChangeSet receivedChangeSet
    Throwable receivedError

    private CountDownLatch changeSetReceived
    private final CountDownLatch subscribed = new CountDownLatch(1)
    private final CountDownLatch completed = new CountDownLatch(1)
    private final CountDownLatch errorReceived = new CountDownLatch(1)

    boolean awaitSubscribed(long milliseconds = 1000) {
        subscribed.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    void expectChangeSet() {
        receivedChangeSet = null
        changeSetReceived = new CountDownLatch(1)
    }

    boolean awaitChangeSet(long milliseconds = 1000) {
        changeSetReceived.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    boolean awaitCompleted(long milliseconds = 1000) {
        completed.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    void expectError() {
        receivedError = null
    }

    boolean awaitError(long milliseconds = 1000) {
        errorReceived.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    @Override
    void onSubscribe(final Subscription s) {
        subscription = s
        subscribed.countDown()
    }

    @Override
    void onNext(final CacheChangeSet cacheChangeSet) {
        receivedChangeSet = cacheChangeSet
        changeSetReceived.countDown()
    }

    @Override
    void onError(final Throwable t) {
        receivedError = t
        errorReceived.countDown()
    }

    @Override
    void onComplete() {
        completed.countDown()
    }
}
