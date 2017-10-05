// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util

import com.fasterxml.jackson.databind.JsonNode

object ScalaJsonCacheModule extends JsonCacheModule {

  override def getJsonCache(
    cacheId: String, publisherBacklogLimit: Int
  ): JsonCache = null

  override def getJsonCache(
    cacheId: String, publisherBacklogLimit: Int,
    cacheObjects: util.Set[CacheObject]
  ): JsonCache = null

  override def getCacheLocation(
    cacheObjectId: String
  ): CacheLocation = ScalaCacheLocation(cacheObjectId)

  override def getCacheObject(
    cacheObjectId: String, cacheObjectType: String,
    cacheObjectContent: JsonNode
  ): CacheObject = ScalaCacheObject(cacheObjectId, cacheObjectType, cacheObjectContent)

  override def getCacheChangeSet(
    puts: util.List[CacheObject],
    removes: util.List[CacheLocation]
  ): CacheChangeSet = null

  override def getCacheChanger(
    cacheChangeSet: CacheChangeSet
  ): CacheChanger = null
}
