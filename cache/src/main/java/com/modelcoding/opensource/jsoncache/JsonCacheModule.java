// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * A {@link JsonCacheModule} provides an implementation of the JsonCache API.
 * <p>
 * The API is defined using interfaces, making it possible to stack/decorate implementations.<br>
 * (Adding logging to a base implementation, for example).    
 */
public interface JsonCacheModule {

    /**
     * @param cacheObjectId an id for the {@link CacheObject} - cannot be {@code null}
     * @param cacheObjectType a type for the {@link CacheObject} - cannot be {@code null}
     * @param cacheObjectContent some content for the {@link CacheObject} - cannot be {@code null}
     * @return an instance of a {@link CacheObject} with the given {@code cacheObjectId}, {@code cacheObjectType} and
     *         {@code cacheObjectContent}
     * @throws NullPointerException if:
     *         <ul>
     *             <li>{@code cacheObjectId} is {@code null}</li>
     *             <li>{@code cacheObjectType} is {@code null}</li>
     *             <li>{@code cacheObjectContent} is {@code null}</li>
     *         </ul>     
     */
    CacheObject getCacheObject(String cacheObjectId, String cacheObjectType, JsonNode cacheObjectContent);

    /**
     * @param cacheObjectId an id for the {@link CacheRemove} - cannot be {@code null}
     * @param cacheRemoveContent some content for the {@link CacheRemove} - cannot be {@code null}
     * @return an instance of a {@link CacheRemove} with the given {@code cacheObjectId} and {@code cacheRemoveContent}
     * @throws NullPointerException if {@code cacheObjectId} is {@code null}, or {@code cacheRemoveContent} is {@code null}
     */
    CacheRemove getCacheRemove(String cacheObjectId, JsonNode cacheRemoveContent);

    /**
     * @param cacheObjectId an id for the {@link CacheRemove} - cannot be {@code null}
     * @return an instance of a {@link CacheRemove} with the given {@code cacheObjectId} and with an empty 
     *         {@link com.fasterxml.jackson.databind.node.ObjectNode} as its {@link CacheRemove#getContent()}
     * @throws NullPointerException if {@code cacheObjectId} is {@code null}
     */
    CacheRemove getCacheRemove(String cacheObjectId);

    /**
     * @param puts put operations on a {@link JsonCache} - cannot be {@code null}
     * @param removes remove operations on a {@link JsonCache} - cannot be {@code null}
     * @param isCacheImage sets the result of {@link CacheChangeSet#isCacheImage()} on the returned {@link CacheChangeSet}               
     * @return an instance of a {@link CacheChangeSet} with the given {@code puts} and {@code removes}
     * @throws NullPointerException if {@code puts} is {@code null}, or {@code removes} is {@code null}
     */
    CacheChangeSet getCacheChangeSet(Set<? extends CacheObject> puts, Set<? extends CacheRemove> removes, boolean isCacheImage);
    
    /**
     * @param cacheObjects set of objects for the {@link Cache} - cannot be {@code null}
     * @return an instance of a {@link Cache} containing the given {@code cacheObjects}
     * @throws NullPointerException if {@code cacheObjects} is {@code null}
     */
    Cache getCache(Set<? extends CacheObject> cacheObjects);

    /**
     * @param cacheChangeSet changes to be applied to a {@link JsonCache} - cannot be {@code null}
     * @return an instance of a {@link CacheChangeCalculator} that will simply apply all the puts and removes from the given
     *         {@code cacheChangeSet}, returning the given {@code cacheChangeSet} as the changes applied
     * @throws NullPointerException if {@code cacheChangeSet} is {@code null}        
     * @throws IllegalArgumentException if the given {@code cacheChangeSet} has {@link CacheChangeSet#isCacheImage()} as {@code true}
     */
    CacheChangeCalculator getCacheChangeCalculator(CacheChangeSet cacheChangeSet);

    /**
     * @param cacheId an id for the {@link JsonCache} - cannot be {@code null}
     * @param subscriberBacklogLimit limit of buffered notifications beyond which a slow subscriber is completed and dropped
     *                               - cannot be negative or 0
     * @param cache initial set of objects for the {@link JsonCache} - cannot be {@code null}
     * @return an instance of a {@link JsonCache} with the given {@code cacheId}, {@code subscriberBacklogLimit} and
     *         initially containing the given {@code cache}
     * @throws NullPointerException if {@code cacheId} is {@code null}, or {@code cache} is {@code null}       
     * @throws IllegalArgumentException if {@code subscriberBacklogLimit} is negative or 0
     */
    JsonCache getJsonCache(String cacheId, int subscriberBacklogLimit, Cache cache);
}
