// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.controlledclient;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public interface RestartableSubscriber<T> extends Subscriber<T> {

    void restartSubscriptionWith(Publisher<T> publisher);
}
