// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.controlledclient;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheObject;
import com.modelcoding.opensource.jsoncache.JsonCache;
import com.modelcoding.opensource.jsoncache.JsonCacheModule;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.function.Predicate;

public interface ControlledClientModule {

    /**
     * Creates a {@link CacheChangeSetProcessor} with the following properties:<br>
     * <ul>
     *     <li>subscribes to the given {@code cacheObjectSelectors} when its subscriber is attached 
     *     - i.e. on the call to {@link CacheChangeSetProcessor#subscribe(Subscriber)}</li>
     *     <li>receives {@link CacheObject} selectors one at a time, and maintains a current selector</li>
     *     <li>on receiving a new selector, calls {@link JsonCache#sendImageToSubscriber(Subscriber)} on the cache 
     *     provided to it by {@link CacheChangeSetProcessor#connect(JsonCache, Publisher)}<br>
     *     - this ensures that a new cache image {@link CacheChangeSet} for the contents of the {@link JsonCache} is published -<br>
     *     the new selector becomes the pending selector change</li>
     *     <li>the pending selector change is made the current selector on receipt of the next cache image {@link CacheChangeSet}</li>
     *     <li>if another selector is received whilst waiting for a cache image, this latest selector becomes the pending 
     *     selector change, replacing the old pending change</li>
     *     <li>on receiving any {@link CacheChangeSet}, the current selector is used to create a new {@link CacheChangeSet}
     *     for publication</li>
     *     <li>each output {@link CacheChangeSet} created always contains all the removes from the input change set</li>
     *     <li>each put from an input change set is passed to the current selector</li>
     *     <li>if a put is selected, the put is added to the puts for the output change set</li>
     *     <li>if a put is not selected, or there is no current selector, the put is converted to a "basic" remove 
     *     (see {@link JsonCacheModule#getCacheRemove(String)}), and added to the removes for the output change set</li>
     *     <li>if the publication of the cache object selectors is finished (by completion or error), the subscription 
     *     to the cache change sets is cancelled</li>
     *     <li>likewise, if the subscription to the cache change sets is finished (by completion or error), the
     *     subscription to the cache object selectors is cancelled</li>
     * </ul><br>
     * In this way, a subscriber to the {@link CacheChangeSetProcessor} returned here can maintain a copy of the 
     * contents of the {@link JsonCache} being controlled, filtered according to the {@code cacheObjectSelectors} 
     * provided.
     *
     * @param cacheObjectSelectors stream of cache object selectors, used to re-write {@link CacheChangeSet}s.
     * @return a processor which receives and processes a stream of {@link CacheChangeSet}s to its subscriber, 
     *         re-writing {@link CacheChangeSet}s according to the {@code cacheObjectSelectors} provided.
     * @throws IllegalArgumentException if {@code cacheObjectSelectors} is {@code null}        
     */
    CacheChangeSetProcessor getCacheChangeSetProcessor(Publisher<Predicate<CacheObject>> cacheObjectSelectors);

    ControlledCacheChangeSetSource getControlledCacheChangeSetSource(
        JsonCache jsonCache,
        CacheChangeSetProcessor cacheObjectSelector,
        CacheChangeSetProcessor cacheObjectAuthorisor
    );
}
