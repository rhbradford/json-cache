// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util

import com.fasterxml.jackson.databind.JsonNode

object ScalaJsonCacheModule extends JsonCacheModule {

  override def getJsonCache(
    cacheId: String, publisherBacklogLimit: Int,
    cacheObjects: util.Set[_ <: CacheObject]
  ): JsonCache = null

  override def getPutObject(
    cacheObjectId: String, cacheObjectType: String,
    cacheObjectContent: JsonNode
  ): PutObject = ScalaPutObject(cacheObjectId, cacheObjectType, cacheObjectContent)

  override def getRemoveObject(
    cacheObjectId: String, removeObjectContent: JsonNode
  ): RemoveObject = null

  override def getCacheChangeSet(
    puts: util.Set[PutObject],
    removes: util.Set[RemoveObject]
  ): CacheChangeSet = null

  override def getCacheChanger(
    cacheChangeSet: CacheChangeSet
  ): CacheChanger = null
}
