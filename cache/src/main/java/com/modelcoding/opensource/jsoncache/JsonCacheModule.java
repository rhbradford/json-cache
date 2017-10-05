// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;

public interface JsonCacheModule {

    JsonCache getJsonCache(String cacheId, int publisherBacklogLimit);

    JsonCache getJsonCache(String cacheId, int publisherBacklogLimit, Set<CacheObject> cacheObjects);

    CacheLocation getCacheLocation(String cacheObjectId);

    CacheObject getCacheObject(String cacheObjectId, String cacheObjectType, JsonNode cacheObjectContent);

    CacheChangeSet getCacheChangeSet(List<CacheObject> puts, List<CacheLocation> removes);

    CacheChanger getCacheChanger(CacheChangeSet cacheChangeSet);
}
