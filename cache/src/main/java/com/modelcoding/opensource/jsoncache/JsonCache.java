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
 * A {@link JsonCache} receives commands that can access and may alter the set of objects it contains via 
 * {@link #onNext(CacheFunction)}.<br>
 * The {@link CacheFunction}s received are applied in the same sequence as they were supplied.
 * <p>
 * A {@link JsonCache} publishes the changes made to the set of objects it contains as a sequence of {@link CacheChangeSet}s to
 * {@link Subscriber}s registered using {@link #subscribe(Subscriber)}.<br>     
 * A {@link Subscriber} receives an initial cache image {@link CacheChangeSet} containing a {@link CacheObject} "put" 
 * for each object in the cache when publication to the subscriber is started. This initial {@link CacheChangeSet} has
 * {@link CacheChangeSet#isCacheImage()} as {@code true}.<br>
 * The initial {@link CacheChangeSet} is followed by {@link CacheChangeSet}s detailing subsequent changes made to the 
 * {@link JsonCache} thereafter. These {@link CacheChangeSet}s have {@link CacheChangeSet#isCacheImage()} as {@code false}.<br>
 * A {@link JsonCache} calls {@link Subscriber#onComplete()} if a subscription is cancelled via {@link Subscription#cancel()}.<br>
 * A {@link JsonCache} stops publishing to a subscriber and calls {@link Subscriber#onError(Throwable)} if the backlog of 
 * change sets published to a subscriber exceeds {@link #getSubscriberBacklogLimit()}.
 * <p>
 * A {@link JsonCache} can be used as a {@link Subscriber} to a {@link Publisher} of {@link CacheFunction}s.<br>
 * Used in this way, the termination of the subscription to {@link CacheFunction}s also terminates any
 * {@link CacheChangeSet} subscribers attached to the {@link JsonCache}. However, a {@link JsonCache} keeps a constant 
 * demand for {@link CacheFunction}s to ensure the input of {@link CacheFunction}s into the 
 * {@link JsonCache} is de-coupled from the demand for {@link CacheChangeSet}s.  
 */
public interface JsonCache extends CacheImageSender, Subscriber<CacheFunction> {

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
     * Adds the given {@code cacheFunction} to the sequence of pending functions to be applied in due course.<br>
     * {@link CacheFunction}s are applied in the order received. 
     * <p>
     * A {@link JsonCache} applies a {@link CacheFunction} by calling {@link CacheFunction#execute(Cache)} against its
     * current {@link Cache}.<br>
     * A {@link JsonCache} replaces its current {@link Cache} with {@link CacheFunction.Result#getCache()} if there is
     * a difference between the {@link Cache}s, and outputs {@link CacheFunction.Result#getChangeSet()} to its subscribers.<br>
     * A {@link JsonCache} does not output a {@link CacheChangeSet} to its subscribers if a {@link CacheFunction} does not
     * provide a new {@link Cache}.
     * <p>
     * A {@link JsonCache} immediately requests another {@code cacheFunction} if subscribed.
     * 
     * @param cacheFunction defines a function that can access and may change the contents of this {@link JsonCache}.
     * @throws NullPointerException if {@code cacheFunction} is {@code null}
     */
    @Override
    void onNext(CacheFunction cacheFunction);

    /**
     * A {@link JsonCache} once subscribed immediately requests a {@code cacheChangeCalculator}. 
     * 
     * @param subscription {@link Subscription} that allows requesting data via {@link Subscription#request(long)}
     * @throws NullPointerException if {@code subscription} is {@code null}
     */
    @Override
    void onSubscribe(Subscription subscription);

    /**
     * A {@link JsonCache} fails all of its {@link CacheChangeSet} subscribers with the given {@code error} if its 
     * subscription to {@link CacheFunction}s fails with an error.
     * <p>
     * A {@link JsonCache} is finished by calling this method. Resources are cleared up, and the {@link JsonCache} 
     * must not be used any more.      
     * 
     * @param error an error with the subscription to {@link CacheFunction}s
     * @throws NullPointerException if {@code error} is {@code null}
     */
    @Override
    void onError(Throwable error);

    /**
     * A {@link JsonCache} completes all of its {@link CacheChangeSet} subscribers if its subscription to 
     * {@link CacheFunction}s completes.
     * <p>
     * A {@link JsonCache} is finished by calling this method. Resources are cleared up, and the {@link JsonCache} 
     * must not be used any more.      
     */
    @Override
    void onComplete();
    
    /**
     * Register a {@link Subscriber} to receive {@link CacheChangeSet}s from this JsonCache.
     * <p>
     * A {@link Subscriber} receives an initial cache image {@link CacheChangeSet} containing a {@link CacheObject} "put" 
     * for each object in the cache when publication to the subscriber is started. The initial {@link CacheChangeSet} has
     * {@link CacheChangeSet#isCacheImage()} as {@code true}.<br>
     * The initial {@link CacheChangeSet} is followed by {@link CacheChangeSet}s detailing subsequent changes made to the 
     * {@link JsonCache} thereafter. These {@link CacheChangeSet}s have {@link CacheChangeSet#isCacheImage()} as {@code false}.<br>
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
     * All calls to {@link #onNext(CacheFunction)} that happened before this call have their resulting {@link CacheChangeSet}s 
     * output to the {@code subscriber} prior to the requested cache image.
     *     
     * @param subscriber the subscriber to receive the cache image.
     * @throws NullPointerException if {@code subscriber} is {@code null}
     */
    void sendImageToSubscriber(Subscriber<? super CacheChangeSet> subscriber);
}
