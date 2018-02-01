// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages.testSupport

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MockSubscriber<T> implements Subscriber<T> {

    Subscription subscription

    List<T> receivedObjects = []
    Throwable receivedError

    private CountDownLatch objectsReceived = new CountDownLatch(0)
    private final CountDownLatch subscribed = new CountDownLatch(1)
    private final CountDownLatch completed = new CountDownLatch(1)
    private final CountDownLatch errorReceived = new CountDownLatch(1)

    boolean awaitSubscribed(long milliseconds = 1000) {
        subscribed.await(milliseconds, TimeUnit.MILLISECONDS)
    }

    void expectObjects(int numObjects = 1) {
        receivedObjects = []
        objectsReceived = new CountDownLatch(numObjects)
    }

    boolean awaitObjects(long milliseconds = 1000) {
        objectsReceived.await(milliseconds, TimeUnit.MILLISECONDS)
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
    void onNext(final T object) {
        receivedObjects << object
        objectsReceived.countDown()
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
