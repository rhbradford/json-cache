// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

object ScalaJsonCacheModule extends JsonCacheModule {

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
    new ScalaCacheChangeSet(puts, removes)

  override def getCacheChanger(
    cacheChangeSet: CacheChangeSet
  ): CacheChanger = 
    null

  override def getCache(
    cacheObjects: util.Set[_ <: CacheObject]
  ): Cache =
    null

  override def getJsonCache(
    cacheId: String, subscriberBacklogLimit: Int,
    cache: Cache
  ): JsonCache =
    null

  override def getJsonCache(
    cacheId: String, subscriberBacklogLimit: Int
  ): JsonCache =
    null
}
