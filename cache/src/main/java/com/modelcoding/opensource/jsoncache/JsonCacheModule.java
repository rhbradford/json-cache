// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * A JsonCacheModule provides an implementation of the {@link JsonCache} API.
 * <p>
 * The API is defined using interfaces, making it possible to stack/decorate implementations.<br>
 * (Adding logging to a base implementation, for example).    
 */
public interface JsonCacheModule {

    JsonCache getJsonCache(String cacheId, int publisherBacklogLimit, Set<? extends CacheObject> cacheObjects);

    PutObject getPutObject(String cacheObjectId, String cacheObjectType, JsonNode cacheObjectContent);

    RemoveObject getRemoveObject(String cacheObjectId, JsonNode removeObjectContent);

    CacheChangeSet getCacheChangeSet(Set<? extends PutObject> puts, Set<? extends RemoveObject> removes);

    CacheChanger getCacheChanger(CacheChangeSet cacheChangeSet);
}
