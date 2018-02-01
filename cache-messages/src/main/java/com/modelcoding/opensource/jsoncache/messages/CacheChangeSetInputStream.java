// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheMessage;
import com.modelcoding.opensource.jsoncache.CacheObject;
import com.modelcoding.opensource.jsoncache.CacheRemove;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A {@link CacheChangeSetInputStream} converts a flow of {@link CacheMessage}s into a flow of {@link CacheChangeSet}s.
 * <p>
 * Calling {@link #getCacheMessageSubscriber(Observer)} returns a {@link Subscriber} which will call back to 
 * {@link Observer#onSubscribed(Publisher)} once it is subscribed to a source of {@link CacheMessage}s, providing
 * a {@link Publisher} of {@link CacheChangeSet}s.<br>
 * The demand from a subscriber to the {@link Publisher} of {@link CacheChangeSet}s is passed back to the source of
 * {@link CacheMessage}s - once there is some demand for a {@link CacheChangeSet} a {@link CacheMessage}
 * will be requested.
 * <p>
 * Each {@link CacheChangeSet} is converted from a stream of {@link CacheMessage}s, by assembling from the frame:
 * <ul>
 *     <li>a {@link StartOfCacheChangeSet}</li>
 *     <li>one or more {@link CacheObject}s for the puts</li>
 *     <li>one or more {@link CacheRemove}s for the removes</li>
 *     <li>an {@link EndOfCacheChangeSet}</li>
 * </ul>    
 */
public interface CacheChangeSetInputStream {

    @FunctionalInterface
    interface Observer {

        /**
         * This is called only once.
         * 
         * @param changeSetPublisher a publisher of {@link CacheChangeSet}s.<br>
         *                           The {@link Publisher} can only be subscribed to <em>once</em>.<br>
         *                           The {@link Subscriber} attached to the {@link Publisher} terminates with
         *                           {@link Subscriber#onError(Throwable)} if the source of {@link CacheMessage}s
         *                           terminates with an error.<br>
         *                           The {@link Subscriber} attached to the {@link Publisher} terminates with
         *                           {@link Subscriber#onComplete()} if the source of {@link CacheMessage}s
         *                           completes.<br>
         */
        void onSubscribed(Publisher<CacheChangeSet> changeSetPublisher);
    }

    /**
     * Returns a {@link Subscriber} to {@link CacheMessage}s. When this subscriber is subscribed to a source of 
     * {@link CacheMessage}s, a {@link Publisher} of {@link CacheChangeSet}s is created and passed to the given
     * {@code observer}.
     * <p>
     * The demand from a subscriber to the {@link Publisher} of {@link CacheChangeSet}s is passed back to the source of
     * {@link CacheMessage}s - once there is some demand for a {@link CacheChangeSet} a {@link CacheMessage}
     * will be requested.
     * <p>
     * The sequence of {@link CacheMessage}s is converted into a stream of {@link CacheChangeSet}s, by assembling
     * from the frame:
     * <ul>
     *     <li>a {@link StartOfCacheChangeSet}</li>
     *     <li>one or more {@link CacheObject}s for the puts</li>
     *     <li>one or more {@link CacheRemove}s for the removes</li>
     *     <li>an {@link EndOfCacheChangeSet}</li>
     * </ul>
     * An error in the stream of {@link CacheMessage}s will result in the subscription being cancelled, and the output
     * of {@link CacheChangeSet}s being terminated with {@link Subscriber#onError(Throwable)}.
     * 
     * @param observer will receive a {@link Publisher} of {@link CacheChangeSet}s once the returned {@link Subscriber}
     *                 has received its {@link Subscription}
     * @return a {@link Subscriber} to be attached to a {@link Publisher} of {@link CacheMessage}s
     * @throws NullPointerException if {@code observer} is {@code null}
     * @throws IllegalStateException if called more than once
     */
    Subscriber<? extends CacheMessage> getCacheMessageSubscriber(Observer observer);
}
