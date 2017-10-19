// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheImage;
import com.modelcoding.opensource.jsoncache.JsonCache;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * A {@link CacheChangeSetProcessor} subscribes to a stream of {@link CacheChangeSet}s, processes each one into (possibly)
 * another {@link CacheChangeSet}, and publishes this to its subscribers.
 * <p>
 * A {@link CacheChangeSetProcessor} is provided with an input {@link Publisher} by a call to 
 * {@link #connect(JsonCache, Publisher)}.<br>
 * A subscription to the {@link CacheChangeSetProcessor} triggers it to in turn subscribe to its input publisher.<br>
 * A subscription attempted when no input publisher exists results in an {@link IllegalStateException}.      
 * A {@link CacheChangeSetProcessor} only supports a single subscription - subsequent subscription attempts result in an
 * {@link IllegalStateException}.
 * <p>
 * All calls to the subscription to this {@link CacheChangeSetProcessor} are forwarded in turn to its subscription to the 
 * input publisher.
 * <p>
 * A {@link CacheChangeSetProcessor} can request {@link JsonCache#sendImageToSubscriber(Subscriber)} to have a
 * {@link CacheImage} be sent into the stream.<br>
 * A received {@link CacheImage} always results in the output of another {@link CacheImage}.  
 */
public interface CacheChangeSetProcessor extends Publisher<CacheChangeSet> {

    /**
     * Links this {@link CacheChangeSetProcessor} with a {@link JsonCache} 
     * (in order to request {@link CacheImage}s via {@link JsonCache#sendImageToSubscriber(Subscriber)}),
     * and provides an {@code input} source of {@link CacheChangeSet}s.
     * <p>
     * Note that the {@code input} publisher is not subscribed to until a subscription is made to this processor.
     * 
     * @param jsonCache the original source of {@link CacheChangeSet}s
     * @param input the input source of {@link CacheChangeSet}s for this processor
     */
    void connect(JsonCache jsonCache, Publisher<CacheChangeSet> input);

    /**
     * Causes this {@link CacheChangeSetProcessor} to subscribe to its input {@link Publisher} and begin processing 
     * {@link CacheChangeSet}s through to the given {@code subscriber}.
     * 
     * @param subscriber the {@link Subscriber} that will consume signals from this {@link CacheChangeSetProcessor}
     * @throws IllegalStateException if called more than once, or no input publisher is available    
     */
    @Override
    void subscribe(Subscriber<? super CacheChangeSet> subscriber);
}
