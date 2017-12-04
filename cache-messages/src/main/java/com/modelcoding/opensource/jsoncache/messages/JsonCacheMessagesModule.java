// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages;

import com.fasterxml.jackson.databind.JsonNode;
import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheMessage;

public interface JsonCacheMessagesModule {

    /**
     * @param cacheChangeSet the {@link CacheChangeSet} being framed
     * @return the {@link CacheMessage} for the start of a sequence of messages representing the given {@code cacheChangeSet}
     * @throws NullPointerException if {@code cacheChangeSet} is {@code null}        
     */
    StartOfCacheChangeSet getStartOfCacheChangeSet(CacheChangeSet cacheChangeSet);

    /**
     * @param json JSON representation of a {@link StartOfCacheChangeSet} in the form given by {@link StartOfCacheChangeSet#asJsonNode()}
     * @return an instance of a {@link StartOfCacheChangeSet} as defined by the given {@code json}
     * @throws NullPointerException if {@code json} is {@code null}
     * @throws IllegalArgumentException if {@code json} is not in the form given by {@link StartOfCacheChangeSet#asJsonNode()}
     */
    StartOfCacheChangeSet getStartOfCacheChangeSet(JsonNode json);
    
    /**
     * @param cacheChangeSet the {@link CacheChangeSet} being framed
     * @return the {@link CacheMessage} for the end of a sequence of messages representing the given {@code cacheChangeSet}
     * @throws NullPointerException if {@code cacheChangeSet} is {@code null}        
     */
    EndOfCacheChangeSet getEndOfCacheChangeSet(CacheChangeSet cacheChangeSet);

    /**
     * @param json JSON representation of a {@link EndOfCacheChangeSet} in the form given by {@link EndOfCacheChangeSet#asJsonNode()}
     * @return an instance of a {@link EndOfCacheChangeSet} as defined by the given {@code json}
     * @throws NullPointerException if {@code json} is {@code null}
     * @throws IllegalArgumentException if {@code json} is not in the form given by {@link EndOfCacheChangeSet#asJsonNode()}
     */
    EndOfCacheChangeSet getEndOfCacheChangeSet(JsonNode json);
    
    /**
     * @param cacheChangeSet a {@link CacheChangeSet} to be converted to a framed sequence of {@link CacheMessage}s
     * @return a {@link CacheChangeSetFrame} to convert the given {@code cacheChangeSet} into a framed sequence of
     *         {@link CacheMessage}s
     * @throws NullPointerException if {@code cacheChangeSet} is {@code null}        
     */
    CacheChangeSetFrame getCacheChangeSetFrame(CacheChangeSet cacheChangeSet);

    /**
     * @return a {@link CacheChangeSetOutputStream} to provide a means of subscribing to {@link CacheChangeSet}s and
     *         re-publishing them as {@link CacheMessage}s
     */
    CacheChangeSetOutputStream getCacheChangeSetOutputStream();

    /**
     * @return a stateful entity that can assemble {@link CacheChangeSetFrame}s from {@link CacheMessage}s 
     */
    CacheChangeSetFrameAssembler getCacheChangeSetFrameAssembler();
    
    /**
     * @param frameAssembler a {@link CacheChangeSetFrameAssembler} to assemble {@link CacheMessage}s into
     *                       {@link CacheChangeSetFrame}s
     * @return a {@link CacheChangeSetInputStream} to provide a means of subscribing to {@link CacheMessage}s and
     *         re-publishing them as {@link CacheChangeSet}s
     */
    CacheChangeSetInputStream getCacheChangeSetInputStream(CacheChangeSetFrameAssembler frameAssembler);
}
