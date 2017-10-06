// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * A JsonCache is a "live" cache of entities in the form of "JSON objects" - objects that have an id, a type and content 
 * as JSON.
 * <p>
 * A JsonCache contains a {@link Cache} of {@link CacheObject}s.
 * <p>
 * A JsonCache receives commands to alter the set of objects it contains via {@link #applyChanges(CacheChanger)}.<br>
 * The {@link CacheChanger}s received are applied in the same sequence as they were supplied.
 * <p>
 * A JsonCache publishes the changes made to the set of objects it contains as a sequence of {@link CacheChangeSet}s.<br>
 * A {@link Subscriber} receives an initial {@link CacheChangeSet} containing a {@link PutObject} for each object in the
 * cache when publication to the subscriber is started, followed by change sets detailing subsequent changes made to the
 * JsonCache thereafter.<br>
 * A JsonCache will always call {@link Subscriber#onComplete()} at some point once a subscriber has cancelled subscription.<br>
 * A JsonCache will stop publishing to a subscriber and call {@link Subscriber#onComplete()} if the backlog of change sets
 * published to a subscriber exceeds {@link #getPublisherBacklogLimit()}.
 */
public interface JsonCache extends Publisher<CacheChangeSet> {

    /**
     * @return the identity of this JsonCache
     */
    String getId();

    /**
     * @return the maximum number of {@link CacheChangeSet}s allowed in the publication buffer to a {@link Subscriber}
     *         after which the subscriber is dropped by the JsonCache and sent {@link Subscriber#onComplete()}
     */
    int getPublisherBacklogLimit();

    /**
     * Adds the given {@code cacheChanger} to the sequence of pending changes to be applied in due course, that will alter
     * the set of objects contained in this JsonCache. {@link CacheChanger}s are applied in the order received.
     * 
     * @param cacheChanger the changes to be applied to this JsonCache.
     */
    void applyChanges(CacheChanger cacheChanger);

    /**
     * Register a {@link Subscriber} to receive {@link CacheChangeSet}s from this JsonCache.
     * <p>
     * A subscriber receives an initial {@link CacheChangeSet} containing a {@link PutObject} for each object 
     * in the cache when publication to the subscriber is started, followed by change sets detailing subsequent changes 
     * made to the JsonCache thereafter.<br>
     * A JsonCache will always call {@link Subscriber#onComplete()} at some point once a subscriber has cancelled 
     * subscription.<br>
     * A JsonCache will stop publishing to a subscriber and call {@link Subscriber#onComplete()} if the backlog of change 
     * sets published to a subscriber exceeds {@link #getPublisherBacklogLimit()}.
     * 
     * @param s the {@link Subscriber} that will consume {@link CacheChangeSet}s from this JsonCache.
     */
    @Override
    void subscribe(Subscriber<? super CacheChangeSet> s);
}