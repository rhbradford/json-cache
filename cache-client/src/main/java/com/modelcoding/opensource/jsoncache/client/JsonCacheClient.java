// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.JsonCache;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * A {@link JsonCacheClient} provides a live "view" of a {@link JsonCache}.
 * <p>
 * A {@link JsonCacheClient} can be subscribed to only once - subsequent subscriptions result in an 
 * {@link IllegalStateException}.
 */
public interface JsonCacheClient extends Publisher<CacheChangeSet> {

    JsonCache getJsonCache();

    /**
     * Causes this {@link JsonCacheClient} to subscribe to its {@link #getJsonCache()}, and begin processing 
     * {@link CacheChangeSet}s through to the given {@code subscriber}.
     * 
     * @param subscriber the {@link Subscriber} that will consume signals from this {@link JsonCacheClient}
     * @throws NullPointerException if {@code subscriber} is {@code null}
     * @throws IllegalStateException if called more than once    
     */
    @Override
    void subscribe(Subscriber<? super CacheChangeSet> subscriber);
}
