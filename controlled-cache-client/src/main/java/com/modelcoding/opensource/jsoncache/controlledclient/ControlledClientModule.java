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

    <T> RestartableSubscriber<T> getRestartableSubscriber();

    CacheChangeSetSource.Factory getCacheChangeSetSourceFactory(Publisher<CacheChangeSet> source);
    
    /**
     * Creates a {@link CacheChangeSetController} with the following properties:<br>
     * <ul>
     *     <li>subscribes to the given {@code cacheObjectSelectors} when its subscriber is attached 
     *     - i.e. on the call to {@link CacheChangeSetController#subscribe(Subscriber)}</li>
     *     <li>requests {@link CacheObject} selectors one at a time, and maintains a current selector</li>
     *     <li>on receiving a new selector, calls {@link CacheChangeSetSource#restart()} on the source provided to it
     *     by {@link CacheChangeSetController#connect(CacheChangeSetSource, Publisher)}
     *     - this ensures that a new {@link CacheChangeSet} for the contents of a {@link JsonCache} is published</li>
     *     <li>on receiving a {@link CacheChangeSet}, the current selector is used to create a new {@link CacheChangeSet}
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
     * In this way, a subscriber to the {@link CacheChangeSetController} returned here can maintain a copy of the 
     * contents of the {@link JsonCache} being controlled, filtered according to the {@code cacheObjectSelectors} 
     * provided.
     *
     * @param cacheObjectSelectors stream of cache object selectors, used to re-write {@link CacheChangeSet}s
     * @return a controller, which can be used to subscribe to a {@link JsonCache} to maintain a copy of the cache 
     *         contents, filtered according to the {@code cacheObjectSelectors} provided  
     */
    CacheChangeSetController getCacheChangeSetController(Publisher<Predicate<CacheObject>> cacheObjectSelectors);

    ControlledCacheChangeSetSource getControlledCacheChangeSetSource(
        String id,
        CacheChangeSetSource.Factory cacheChangeSetSourceFactory,
        CacheChangeSetController cacheObjectSelector,
        CacheChangeSetController cacheObjectAuthorisor
    );
}
