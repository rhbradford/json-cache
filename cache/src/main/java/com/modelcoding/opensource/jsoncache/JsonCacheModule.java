// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * A JsonCacheModule provides an implementation of the JsonCache API.
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
     * @throws IllegalArgumentException if any of the following is true:<br>
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
     * @throws IllegalArgumentException if any of the following is true:<br>
     *         <ul>
     *             <li>{@code cacheObjectId} is {@code null}</li>
     *             <li>{@code cacheRemoveContent} is {@code null}</li>
     *         </ul>     
     */
    CacheRemove getCacheRemove(String cacheObjectId, JsonNode cacheRemoveContent);

    /**
     * @param cacheObjectId an id for the {@link CacheRemove} - cannot be {@code null}
     * @return an instance of a {@link CacheRemove} with the given {@code cacheObjectId} and with an empty 
     *         {@link com.fasterxml.jackson.databind.node.ObjectNode} as its {@link CacheRemove#getContent()}
     * @throws IllegalArgumentException if {@code cacheObjectId} is {@code null}
     */
    CacheRemove getCacheRemove(String cacheObjectId);

    /**
     * @param puts put operations on a {@link JsonCache} - cannot be {@code null}
     * @param removes remove operations on a {@link JsonCache} - cannot be {@code null}
     * @param isCacheImage if {@code true}, indicates the CacheChangeSet returned contains a "put" for each object in a 
     *                  {@link JsonCache} and no "removes"               
     * @return an instance of a {@link CacheChangeSet} with the given {@code puts} and {@code removes}
     * @throws IllegalArgumentException if any of the following is true:<br>
     *         <ul>
     *             <li>{@code puts} is {@code null}</li>
     *             <li>{@code removes} is {@code null}</li>
     *             <li>{@code isCacheImage} is {@code true}, but {@code removes} is not empty</li>
     *         </ul>     
     */
    CacheChangeSet getCacheChangeSet(Set<? extends CacheObject> puts, Set<? extends CacheRemove> removes, boolean isCacheImage);

    /**
     * @param cacheObjects set of objects for the {@link Cache} - cannot be {@code null}
     * @return an instance of a {@link Cache} containing the given {@code cacheObjects}
     * @throws IllegalArgumentException if {@code cacheObjects} is {@code null}
     */
    Cache getCache(Set<? extends CacheObject> cacheObjects);

    /**
     * @param cacheChangeSet changes to be applied to a {@link JsonCache} - cannot be {@code null}
     * @return an instance of a {@link CacheChangeCalculator} that will simply apply all the puts and removes from the given
     *         {@code cacheChangeSet}, returning the given {@code cacheChangeSet} as the changes applied 
     * @throws IllegalArgumentException if any of the following is true:<br>
     *         <ul>
     *             <li>{@code cacheChangeSet} is {@code null}</li>
     *             <li>{@link CacheChangeSet#isCacheImage()} is {@code true} for the given {@code cacheChangeSet}</li>
     *         </ul>     
     */
    CacheChangeCalculator getCacheChangeCalculator(CacheChangeSet cacheChangeSet);

    /**
     * @param cacheId an id for the {@link JsonCache} - cannot be {@code null}
     * @param subscriberBacklogLimit limit of buffered notifications beyond which a slow subscriber is completed and dropped
     *                               - cannot be negative or 0
     * @param cache initial set of objects for the {@link JsonCache} - cannot be {@code null}
     * @return an instance of a {@link JsonCache} with the given {@code cacheId}, {@code subscriberBacklogLimit} and
     *         initially containing the given {@code cache}
     * @throws IllegalArgumentException if any of the following is true:<br>
     *         <ul>
     *             <li>{@code cacheId} is {@code null}</li>
     *             <li>{@code subscriberBacklogLimit} is negative or 0</li>
     *             <li>{@code cache} is {@code null}</li>
     *         </ul>     
     */
    JsonCache getJsonCache(String cacheId, int subscriberBacklogLimit, Cache cache);
}
