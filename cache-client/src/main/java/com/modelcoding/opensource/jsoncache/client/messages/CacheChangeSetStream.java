// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client.messages;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheMessage;
import com.modelcoding.opensource.jsoncache.CacheObject;
import com.modelcoding.opensource.jsoncache.CacheRemove;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A {@link CacheChangeSetStream} converts a flow of {@link CacheChangeSet}s into a flow of {@link CacheMessage}s.
 * <p>
 * Calling {@link #getCacheChangeSetSubscriber(Observer)} returns a {@link Subscriber} which will call back to 
 * {@link Observer#onSubscribed(Publisher)} once it is subscribed to a source of {@link CacheChangeSet}s, providing
 * a {@link Publisher} of {@link CacheMessage}s.<br>
 * The demand from a subscriber to the {@link Publisher} of {@link CacheMessage}s is passed back to the source of
 * {@link CacheChangeSet}s - once there is some demand for a {@link CacheMessage} a {@link CacheChangeSet}
 * will be requested.
 * <p>
 * Each {@link CacheChangeSet} is converted into a stream of {@link CacheMessage}s:
 * <ul>
 *     <li>a {@link StartOfCacheChangeSet}</li>
 *     <li>one or more {@link CacheObject}s for the puts</li>
 *     <li>one or more {@link CacheRemove}s for the removes</li>
 *     <li>an {@link EndOfCacheChangeSet}</li>
 * </ul>    
 */
public interface CacheChangeSetStream {

    interface Observer {
        
        void onSubscribed(Publisher<CacheMessage> messagePublisher);
    }

    /**
     * The demand from a subscriber to the {@link Publisher} of {@link CacheMessage}s is passed back to the source of
     * {@link CacheChangeSet}s - once there is some demand for a {@link CacheMessage} a {@link CacheChangeSet}
     * will be requested.
     * <p>
     * Each {@link CacheChangeSet} is converted into a stream of {@link CacheMessage}s:
     * <ul>
     *     <li>a {@link StartOfCacheChangeSet}</li>
     *     <li>one or more {@link CacheObject}s for the puts</li>
     *     <li>one or more {@link CacheRemove}s for the removes</li>
     *     <li>an {@link EndOfCacheChangeSet}</li>
     * </ul>    
     *
     * @param observer will receive a {@link Publisher} of {@link CacheMessage}s once the returned {@link Subscriber}
     *                 has received its {@link Subscription}
     * @return a {@link Subscriber} to be attached to a {@link Publisher} of {@link CacheChangeSet}s
     */
    Subscriber<? extends CacheChangeSet> getCacheChangeSetSubscriber(Observer observer);
}
