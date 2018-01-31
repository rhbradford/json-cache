// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.modelcoding.opensource.jsoncache.CacheMessage;

/**
 * A {@link CacheChangeSetFrameAssembler} is a stateful assembler of {@link CacheMessage}s, outputting 
 * {@link CacheChangeSetFrame}s as they are received.
 * <p>
 * A {@link CacheChangeSetFrameAssembler} is not expected to be thread-safe.    
 */
public interface CacheChangeSetFrameAssembler {

    @FunctionalInterface
    interface Receiver {
        
        void onCacheChangeSetFrame(CacheChangeSetFrame cacheChangeSetFrame);
    }

    /**
     * @param receiver the receiver of assembled {@link CacheChangeSetFrame}s
     * @throws NullPointerException if {@code receiver} is {@code null}
     * @throws IllegalStateException if this method has already been called                    
     */
    void connect(Receiver receiver);
    
    /**
     * Handles the next message from a source.<br>
     * Outputs a {@link CacheChangeSetFrame} to an {@link Receiver} as each frame is completed.    
     * 
     * @param cacheMessage next message from a source
     * @throws IllegalStateException if {@link #connect(Receiver)} has not yet been called                    
     * @throws IllegalArgumentException if the sequence of {@link CacheMessage}s is incorrect                    
     */
    void onCacheMessage(CacheMessage cacheMessage);

    /**
     * Converts JSON into the appropriate {@link CacheMessage}.
     * 
     * @param json a representation of a {@link CacheMessage}
     * @return an instance of the {@link CacheMessage} corresponding to the given {@code json}
     * @throws IllegalArgumentException if {@code json} is not in the correct form
     */
    CacheMessage getCacheMessage(JsonNode json);
}
