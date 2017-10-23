// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A {@link JsonCache} is a "live" cache of entities in the form of "JSON objects".
 * A "JSON object" represents an instance of an entity that has a fixed identity and type, but whose content changes 
 * over time.    
 * <p>
 * A {@link JsonCache} contains a {@link Cache} of {@link CacheObject}s.
 * <p>
 * A {@link JsonCache} receives commands to alter the set of objects it contains via {@link #applyChanges(CacheChangeCalculator)}.<br>
 * The {@link CacheChangeCalculator}s received are applied in the same sequence as they were supplied.
 * <p>
 * A {@link JsonCache} publishes the changes made to the set of objects it contains as a sequence of {@link CacheChangeSet}s to
 * {@link Subscriber}s registered using {@link #subscribe(Subscriber)}.<br>     
 * A {@link Subscriber} receives an initial cache image {@link CacheChangeSet} containing a {@link CacheObject} "put" 
 * for each object in the cache when publication to the subscriber is started. The initial {@link CacheChangeSet} has
 * {@link CacheChangeSet#isCacheImage()} as {@code true}.<br>
 * The initial {@link CacheChangeSet}is followed by {@link CacheChangeSet}s detailing subsequent changes made to the 
 * {@link JsonCache} thereafter. These {@link CacheChangeSet}s hav {@link CacheChangeSet#isCacheImage()} as {@code false}.<br>
 * A subscriber registered using {@link #subscribe(Subscriber)} is notified of all changes entered <em>on the same thread</em>
 * via {@link #applyChanges(CacheChangeCalculator)} after registration.<br>
 * A {@link JsonCache} calls {@link Subscriber#onComplete()} if a subscription is cancelled via {@link Subscription#cancel()}.<br>
 * A {@link JsonCache} stops publishing to a subscriber and calls {@link Subscriber#onError(Throwable)} if the backlog of 
 * change sets published to a subscriber exceeds {@link #getSubscriberBacklogLimit()}.
 */
public interface JsonCache extends Publisher<CacheChangeSet>, CacheImageSender {

    /**
     * @return the identity of this {@link JsonCache}
     */
    String getId();

    /**
     * @return the maximum number of {@link CacheChangeSet}s allowed in the publication buffer to a {@link Subscriber}
     *         after which the subscriber is dropped by the {@link JsonCache} and sent {@link Subscriber#onError(Throwable)}
     */
    int getSubscriberBacklogLimit();
    
    /**
     * Adds the given {@code cacheChangeCalculator} to the sequence of pending changes to be applied in due course, 
     * that will alter the set of objects contained in this {@link JsonCache}.<br>
     * {@link CacheChangeCalculator}s are applied in the order received.
     * 
     * @param cacheChangeCalculator calculates the changes to be applied to this {@link JsonCache}.
     * @throws NullPointerException if {@code cacheChangeCalculator} is {@code null}
     */
    void applyChanges(CacheChangeCalculator cacheChangeCalculator);

    /**
     * Register a {@link Subscriber} to receive {@link CacheChangeSet}s from this JsonCache.
     * <p>
     * A {@link Subscriber} receives an initial cache image {@link CacheChangeSet} containing a {@link CacheObject} "put" 
     * for each object in the cache when publication to the subscriber is started. The initial {@link CacheChangeSet} has
     * {@link CacheChangeSet#isCacheImage()} as {@code true}.<br>
     * The initial {@link CacheChangeSet}is followed by {@link CacheChangeSet}s detailing subsequent changes made to the 
     * {@link JsonCache} thereafter. These {@link CacheChangeSet}s hav {@link CacheChangeSet#isCacheImage()} as {@code false}.<br>
     * A {@link JsonCache} calls {@link Subscriber#onComplete()} if a subscription is cancelled via {@link Subscription#cancel()}.<br>
     * A {@link JsonCache} stops publishing to a subscriber and calls {@link Subscriber#onError(Throwable)} if the backlog of 
     * change sets published to a subscriber exceeds {@link #getSubscriberBacklogLimit()}.
     * 
     * @param subscriber the {@link Subscriber} that will consume {@link CacheChangeSet}s from this {@link JsonCache}.
     * @throws NullPointerException if {@code subscriber} is {@code null}
     */
    @Override
    void subscribe(Subscriber<? super CacheChangeSet> subscriber);

    /**
     * Requests that a {@link CacheChangeSet} representing the contents of this {@link JsonCache}
     * (i.e. containing a "put" for each {@link CacheObject} in the cache, and {@link CacheChangeSet#isCacheImage()} 
     * set as {@code true}) is sent to the given {@code subscriber}.
     * <p>
     * The subscriber is notified of all prior changes entered <em>on the same thread</em> via 
     * {@link #applyChanges(CacheChangeCalculator)} before it receives the cache image {@link CacheChangeSet}.
     *     
     * @param subscriber the subscriber to receive the cache image.
     * @throws NullPointerException if {@code subscriber} is {@code null}
     */
    void sendImageToSubscriber(Subscriber<? super CacheChangeSet> subscriber);
}
