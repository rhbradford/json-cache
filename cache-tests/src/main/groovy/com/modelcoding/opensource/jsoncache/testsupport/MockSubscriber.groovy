// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.testsupport

import com.modelcoding.opensource.jsoncache.CacheChangeSet
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockSubscriber implements Subscriber<CacheChangeSet> {

    private volatile boolean cancelled
    private volatile Subscription subscription
    private CountDownLatch notifications

    private CacheChangeSet specificNotification
    private CountDownLatch specificNotifications

    final List<CacheChangeSet> changeSets = []
    boolean hasCompleted
    boolean hasError

    private final CountDownLatch completed = new CountDownLatch(1)
    private final CountDownLatch subscribed = new CountDownLatch(1)
    private final CountDownLatch errorReceived = new CountDownLatch(1)

    boolean awaitSubscription(long milliseconds = 1000) {
        subscribed.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    void expectChangeSets(int expectedNumberOfNotifications) {
        notifications = new CountDownLatch(expectedNumberOfNotifications)
        changeSets.clear()
    }

    boolean awaitChangeSets(long milliseconds = 1000) {
        notifications.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    void expectChangeSet(CacheChangeSet changeSet) {
        specificNotification = changeSet
        specificNotifications = new CountDownLatch(1)
    }

    boolean awaitChangeSet(long milliseconds = 1000) {
        specificNotifications.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    boolean awaitError(long milliseconds = 1000) {
        errorReceived.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    boolean awaitComplete(long milliseconds = 1000) {
        completed.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    void cancel() {
        cancelled = true
        subscription?.cancel()
    }

    protected void makeRequest(Subscription s) {
        s.request(1)
    }

    @Override
    void onSubscribe(final Subscription s) {
        subscription = s
        // Ensures that MockSubscriber should always receive the initial change set.
        // The initial change set always contains a put for each object currently in the JsonCache.
        makeRequest(subscription)
        subscribed.countDown()
    }

    @Override
    void onNext(final CacheChangeSet cacheChangeSet) {
        changeSets << cacheChangeSet
        notifications?.countDown()
        if(cacheChangeSet == specificNotification)
            specificNotifications?.countDown()
        if(!cancelled)
            makeRequest(subscription)
    }

    @Override
    void onError(final Throwable t) {
        hasError = true
        println t
        errorReceived.countDown()
    }

    @Override
    void onComplete() {
        hasCompleted = true
        completed.countDown()
    }
}
