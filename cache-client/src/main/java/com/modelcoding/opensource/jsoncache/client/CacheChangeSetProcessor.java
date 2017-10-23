// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheImage;
import com.modelcoding.opensource.jsoncache.CacheImageSender;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * A {@link CacheChangeSetProcessor} subscribes to a stream of {@link CacheChangeSet}s, processes each one into (possibly)
 * another {@link CacheChangeSet}, and publishes this to its subscribers.
 * <p>
 * A {@link CacheChangeSetProcessor} is provided with an input {@link Publisher} by a call to 
 * {@link #connect(CacheImageSender)}.<br>
 * A subscription to the {@link CacheChangeSetProcessor} triggers it to in turn subscribe to its input publisher.<br>
 * A {@link CacheChangeSetProcessor} only supports a single subscription.
 * <p>
 * All calls to the subscription to this {@link CacheChangeSetProcessor} are forwarded in turn to its subscription to the 
 * input publisher.
 * <p>
 * A received {@link CacheImage} always results in the output of another {@link CacheImage}, not just a {@link CacheChangeSet}.<br>
 * This ensures that {@link CacheImage}s will flow down through multiple connected {@link CacheChangeSetProcessor}s.     
 */
public interface CacheChangeSetProcessor extends CacheImageSender {

    /**
     * Links this {@link CacheChangeSetProcessor} with an {@code input} source of {@link CacheChangeSet}s.
     * <p>
     * Note that the {@code input} is not subscribed to until a subscription is made to this processor.
     * 
     * @param input the input source of {@link CacheChangeSet}s for this processor
     * @throws NullPointerException if {@code input} is {@code null}
     * @throws IllegalStateException if called more than once    
     */
    void connect(CacheImageSender input);

    /**
     * Causes this {@link CacheChangeSetProcessor} to subscribe to its input {@link Publisher} provided in 
     * {@link #connect(CacheImageSender)} and begin processing {@link CacheChangeSet}s through to the given 
     * {@code subscriber}.
     * 
     * @param subscriber the {@link Subscriber} that will consume signals from this {@link CacheChangeSetProcessor}
     * @throws NullPointerException if {@code subscriber} is {@code null}
     * @throws IllegalStateException if no input publisher is available ({@link #connect(CacheImageSender)} must be called
     *                               first);<br>or if called more than once (a {@link CacheChangeSetProcessor} only supports a 
     *                               single subscription) 
     */
    @Override
    void subscribe(Subscriber<? super CacheChangeSet> subscriber);

    /**
     * Requests that a {@link CacheImage} be sent to the given {@code subscriber}.<br>
     * The request is passed to the input {@link CacheImageSender} provided in {@link #connect(CacheImageSender)}.<br>
     * The subscriber must be the same as that provided in {@link #subscribe(Subscriber)}.    
     *     
     * @param subscriber the subscriber to receive the cache image.
     * @throws NullPointerException if {@code subscriber} is {@code null}
     * @throws IllegalArgumentException if {@code subscriber} is not the same the subscriber as provided in 
     *                                  {@link #subscribe(Subscriber)}
     * @throws IllegalStateException if no input publisher is available ({@link #connect(CacheImageSender)} must be called
     *                               first);<br>or if this {@link CacheChangeSetProcessor} has not yet been subscribed to
     *                               ({@link #subscribe(Subscriber)} must have been called earlier)
     */
    @Override
    void sendImageToSubscriber(Subscriber<? super CacheChangeSet> subscriber);
}
