// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public interface CacheImageSender extends Publisher<CacheChangeSet> {

    /**
     * Requests that a {@link CacheChangeSet} representing the contents of a {@link JsonCache}
     * (i.e. containing a "put" for each {@link CacheObject} in the cache, and {@link CacheChangeSet#isCacheImage()} 
     * set as {@code true}) is sent to the given {@code subscriber}.
     *     
     * @param subscriber the subscriber to receive the cache image.
     * @throws NullPointerException if {@code subscriber} is {@code null}                  
     */
    void sendImageToSubscriber(Subscriber<? super CacheChangeSet> subscriber);
}
