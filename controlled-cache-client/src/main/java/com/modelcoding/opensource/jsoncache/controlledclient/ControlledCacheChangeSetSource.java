// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.controlledclient;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.JsonCache;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A {@link ControlledCacheChangeSetSource} provides a live "view" of a {@link JsonCache}.
 * <p>
 * The view is defined by a {@link CacheChangeSetProcessor} that outputs selected cache objects
 * - {@link #getCacheObjectSelector()}.<br>
 * The view is restricted by a {@link CacheChangeSetProcessor} that outputs permitted cache objects
 * - {@link #getCacheObjectAuthorisor()}.
 * <p>
 * A {@link ControlledCacheChangeSetSource} provides the {@link JsonCache} as the input to the cache object selector.<br>
 * In turn, the cache object selector output is provided as the input to the cache object authorisor.<br>
 * The cache object authorisor output is then the output from the {@link ControlledCacheChangeSetSource}.
 * <p>
 * Subscription to the {@link ControlledCacheChangeSetSource} causes it to subscribe to the cache object authorisor, which
 * subscribes to the cache object selector, which subscribes to the cache.<br>
 * All calls to the {@link Subscription} to the {@link ControlledCacheChangeSetSource} are forwarded to the subscription to
 * the cache object authorisor, which forwards to the subscription to the cache object authorisor, which finally forwards
 * to the subscription to the cache.
 * <p>
 * A {@link ControlledCacheChangeSetSource} can be subscribed to only once - subsequent subscriptions result in an 
 * {@link IllegalStateException}.
 */
public interface ControlledCacheChangeSetSource extends Publisher<CacheChangeSet> {

    JsonCache getJsonCache();

    CacheChangeSetProcessor getCacheObjectSelector();
    
    CacheChangeSetProcessor getCacheObjectAuthorisor();
    
    /**
     * Causes this {@link ControlledCacheChangeSetSource} to subscribe to through its processors to the 
     * {@link #getJsonCache()}, and begin processing {@link CacheChangeSet}s through to the given {@code subscriber}.
     * 
     * @param subscriber the {@link Subscriber} that will consume signals from this {@link ControlledCacheChangeSetSource}
     * @throws IllegalStateException if called more than once    
     */
    @Override
    void subscribe(Subscriber<? super CacheChangeSet> subscriber);
}
