// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages.testSupport

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockSubscriber implements Subscriber<CacheChangeSet> {

    Subscription subscription

    List<CacheChangeSet> receivedChangeSets
    Throwable receivedError

    private CountDownLatch changeSetsReceived
    private final CountDownLatch subscribed = new CountDownLatch(1)
    private final CountDownLatch completed = new CountDownLatch(1)
    private final CountDownLatch errorReceived = new CountDownLatch(1)

    boolean awaitSubscribed(long milliseconds = 1000) {
        subscribed.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    void expectChangeSets(int numChangeSets = 1) {
        receivedChangeSets = []
        changeSetsReceived = new CountDownLatch(numChangeSets)
    }

    boolean awaitChangeSets(long milliseconds = 1000) {
        changeSetsReceived.await(milliseconds, TimeUnit.MILLISECONDS)
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
        receivedChangeSets << cacheChangeSet
        changeSetsReceived.countDown()
    }

    boolean hasError
    
    @Override
    void onError(final Throwable t) {
        hasError = true
        receivedError = t
        errorReceived.countDown()
    }

    boolean hasCompleted
    
    @Override
    void onComplete() {
        hasCompleted = true
        completed.countDown()
    }
}
