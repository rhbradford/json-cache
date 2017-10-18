// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A JsonCache is a "live" cache of entities in the form of "JSON objects".
 * A "JSON object" represents an instance of an entity that has a fixed identity and type, but whose content changes 
 * over time.    
 * <p>
 * A JsonCache contains a {@link Cache} of {@link CacheObject}s.
 * <p>
 * A JsonCache receives commands to alter the set of objects it contains via {@link #applyChanges(CacheChangeCalculator)}.<br>
 * The {@link CacheChangeCalculator}s received are applied in the same sequence as they were supplied.
 * <p>
 * A JsonCache publishes the changes made to the set of objects it contains as a sequence of {@link CacheChangeSet}s to
 * {@link Subscriber}s registered using {@link #subscribe(Subscriber)}.<br>     
 * A {@link Subscriber} receives an initial {@link CacheChangeSet} containing a {@link CacheObject} "put" for each object 
 * in the cache when publication to the subscriber is started ({@link CacheChangeSet#isCacheImage()} is {@code true}), 
 * followed by change sets detailing subsequent changes made to the JsonCache thereafter 
 * ({@link CacheChangeSet#isCacheImage()} is {@code false}).<br>
 * A subscriber registered using {@link #subscribe(Subscriber)} is notified of all changes entered <em>on the same thread</em>
 * via {@link #applyChanges(CacheChangeCalculator)} after registration.<br>
 * A JsonCache calls {@link Subscriber#onComplete()} if a subscription is cancelled via {@link Subscription#cancel()}.<br>
 * A JsonCache stops publishing to a subscriber and calls {@link Subscriber#onError(Throwable)} if the backlog of 
 * change sets published to a subscriber exceeds {@link #getSubscriberBacklogLimit()}.
 */
public interface JsonCache extends Publisher<CacheChangeSet> {

    /**
     * @return the identity of this JsonCache
     */
    String getId();

    /**
     * @return the maximum number of {@link CacheChangeSet}s allowed in the publication buffer to a {@link Subscriber}
     *         after which the subscriber is dropped by the JsonCache and sent {@link Subscriber#onError(Throwable)}
     */
    int getSubscriberBacklogLimit();

    /**
     * Adds the given {@code cacheChangeCalculator} to the sequence of pending changes to be applied in due course, 
     * that will alter the set of objects contained in this JsonCache.<br>
     * {@link CacheChangeCalculator}s are applied in the order received.
     * 
     * @param cacheChangeCalculator calculates the changes to be applied to this JsonCache.
     */
    void applyChanges(CacheChangeCalculator cacheChangeCalculator);

    /**
     * Register a {@link Subscriber} to receive {@link CacheChangeSet}s from this JsonCache.
     * <p>
     * A {@link Subscriber} receives an initial {@link CacheChangeSet} containing a {@link CacheObject} "put" for each object 
     * in the cache when publication to the subscriber is started ({@link CacheChangeSet#isCacheImage()} is {@code true}), 
     * followed by change sets detailing subsequent changes made to the JsonCache thereafter 
     * ({@link CacheChangeSet#isCacheImage()} is {@code false}).<br>
     * A JsonCache calls {@link Subscriber#onComplete()} if a subscription is cancelled via {@link Subscription#cancel()}.<br>
     * A JsonCache stops publishing to a subscriber and calls {@link Subscriber#onError(Throwable)} if the backlog of 
     * change sets published to a subscriber exceeds {@link #getSubscriberBacklogLimit()}.
     * 
     * @param subscriber the {@link Subscriber} that will consume {@link CacheChangeSet}s from this JsonCache.
     */
    @Override
    void subscribe(Subscriber<? super CacheChangeSet> subscriber);

    /**
     * Requests that a {@link CacheChangeSet} containing a {@link CacheObject} "put" for each object in the cache 
     * ({@link CacheChangeSet#isCacheImage()} is {@code true}) is sent to the given {@code subscriber}.
     * <p>
     * The subscriber is notified of all prior changes entered <em>on the same thread</em> via 
     * {@link #applyChanges(CacheChangeCalculator)} before it receives the cache image.
     *     
     * @param subscriber the subscriber to receive the cache image.
     */
    void sendImageToSubscriber(Subscriber<? super CacheChangeSet> subscriber);
}
