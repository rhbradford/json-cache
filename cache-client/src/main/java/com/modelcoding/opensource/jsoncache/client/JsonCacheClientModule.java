// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheImage;
import com.modelcoding.opensource.jsoncache.CacheImageSender;
import com.modelcoding.opensource.jsoncache.CacheObject;
import com.modelcoding.opensource.jsoncache.JsonCache;
import com.modelcoding.opensource.jsoncache.JsonCacheModule;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.function.Predicate;

public interface JsonCacheClientModule {

    /**
     * Creates a {@link CacheChangeSetProcessor} with the following properties:<br>
     * <ul>
     *     <li>subscribes to the {@code cacheObjectSelectors} when a subscriber to this {@link CacheChangeSetProcessor} 
     *     is attached on the call to {@link CacheChangeSetProcessor#subscribe(Subscriber)}</li>
     *     <li>requests {@link CacheObject} selectors one at a time, and maintains a current selector</li>
     *     <li>only subscribes to the {@link CacheImageSender} once the subscription to the selectors has been activated
     *     and a selector has been received</li> 
     *     <li>if the internal subscriber to the {@code cacheObjectSelectors} is completed before any selector
     *     has been received, then the subscriber to this {@link CacheChangeSetProcessor} is completed</li>
     *     <li>if a new selector is received and there is no pending selector change, a {@link CacheImage} is requested 
     *     from the {@link CacheImageSender} and the new selector becomes the pending selector change</li>
     *     <li>the pending selector change is made the current selector on receipt of the next {@link CacheImage}</li>
     *     <li>if another selector is received whilst waiting for a {@link CacheImage}, this latest selector becomes the 
     *     pending selector change, replacing the old pending change</li>
     *     <li>on receiving any {@link CacheChangeSet}, the current selector is used to create a new {@link CacheChangeSet}
     *     for publication</li>
     *     <li>each output {@link CacheChangeSet} created always contains all the removes from the input {@link CacheChangeSet}</li>
     *     <li>each put from an input {@link CacheChangeSet} is passed to the current selector</li>
     *     <li>if a put is selected, the put is added to the puts for the output {@link CacheChangeSet}</li>
     *     <li>if a put is not selected, or there is no current selector, the put is converted to a "basic" remove 
     *     (see {@link JsonCacheModule#getCacheRemove(String)}), and added to the removes for the output {@link CacheChangeSet}</li>
     *     <li>if the internal subscriber to the {@code cacheObjectSelectors} is finished by error, the subscription 
     *     to the {@link CacheImageSender} is cancelled, and the subscriber to this {@link CacheChangeSetProcessor}
     *     receives the error</li>
     *     <li>likewise, if the internal subscriber to the {@link CacheImageSender} is finished by error, the subscription 
     *     to the {@code cacheObjectSelectors} is cancelled, and the subscriber to this {@link CacheChangeSetProcessor}
     *     receives the error</li>
     *     <li>if the subscription to this {@link CacheChangeSetProcessor} is cancelled, the internal subscriptions to 
     *     the {@link CacheImageSender} and {@code cacheObjectSelectors} are cancelled</li> 
     * </ul><br>
     *
     * @param cacheObjectSelectors stream of cache object selectors, used to re-write {@link CacheChangeSet}s.
     * @return a processor which receives and processes a stream of {@link CacheChangeSet}s to its subscriber, 
     *         re-writing {@link CacheChangeSet}s according to the {@code cacheObjectSelectors} provided.
     * @throws NullPointerException if {@code cacheObjectSelectors} is {@code null}        
     */
    CacheChangeSetProcessor getCacheChangeSetProcessor(
        Publisher<Predicate<CacheObject>> cacheObjectSelectors
    );

    JsonCacheClient getControlledCacheChangeSetSource(
        JsonCache jsonCache,
        CacheChangeSetProcessor cacheObjectSelector,
        CacheChangeSetProcessor cacheObjectAuthorisor
    );
}
