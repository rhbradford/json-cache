// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

object ScalaJsonCacheModule extends JsonCacheModule {

  override def getJsonCache(
    cacheId: String, 
    publisherBacklogLimit: Int,
    cacheObjects: util.Set[_ <: CacheObject]
  ): JsonCache = 
    null

  override def getCacheObject(
    cacheObjectId: String, 
    cacheObjectType: String,
    cacheObjectContent: JsonNode
  ): CacheObject = 
    ScalaCacheObject(cacheObjectId)(cacheObjectType, cacheObjectContent)

  override def getCacheRemove(
    cacheObjectId: String, 
    cacheRemoveContent: JsonNode
  ): CacheRemove =
    ScalaCacheRemove(cacheObjectId)(cacheRemoveContent)

  private val emptyContent: JsonNode = new ObjectMapper().createObjectNode()
  
  override def getCacheRemove(
    cacheObjectId: String
  ): CacheRemove =
    ScalaCacheRemove(cacheObjectId)(emptyContent)

  override def getCacheChangeSet(
    puts: util.Set[_ <: CacheObject],
    removes: util.Set[_ <: CacheRemove]
  ): CacheChangeSet =
    null

  override def getCacheChanger(
    cacheChangeSet: CacheChangeSet
  ): CacheChanger = 
    null
}
