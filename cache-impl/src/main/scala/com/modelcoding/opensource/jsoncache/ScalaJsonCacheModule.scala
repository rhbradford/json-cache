// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import java.util

import akka.actor.ActorSystem
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.collection.JavaConverters._

class ScalaJsonCacheModule(implicit val actorSystem: ActorSystem) extends JsonCacheModule {

  import ScalaJsonCacheModule._
  
  override def getCacheObject(
    cacheObjectId: String,
    cacheObjectType: String,
    cacheObjectContent: JsonNode
  ): CacheObject = {

    requireNotNull(cacheObjectId, "A CacheObject cannot have a null id")
    requireNotNull(cacheObjectType, "A CacheObject cannot have a null type")
    requireNotNull(cacheObjectContent, "A CacheObject cannot have null content")

    ScalaCacheObject(cacheObjectId)(cacheObjectType, cacheObjectContent)
  }

  override def getCacheObject(
    json: JsonNode
  ): CacheObject = {
    
    requireNotNull(json, "A CacheObject cannot be created from null json")
    
    ScalaCacheObject(json)
  }

  override def getCacheRemove(
    cacheObjectId: String,
    cacheRemoveContent: JsonNode
  ): CacheRemove = {

    requireNotNull(cacheObjectId, "A CacheRemove cannot have a null id")
    requireNotNull(cacheRemoveContent, "A CacheRemove cannot have null content")

    ScalaCacheRemove(cacheObjectId)(cacheRemoveContent)
  }

  override def getCacheRemove(
    cacheObjectId: String
  ): CacheRemove =
    getCacheRemove(cacheObjectId, emptyContent)

  override def getCacheRemove(
    json: JsonNode
  ): CacheRemove = {
    
    requireNotNull(json, "A CacheRemove cannot be created from null json")
    
    ScalaCacheRemove(json)
  }

  override def getCacheChangeSet(
    puts: util.Set[_ <: CacheObject],
    removes: util.Set[_ <: CacheRemove],
    isCacheImage: Boolean
  ): CacheChangeSet = {

    requireNotNull(puts, "A CacheChangeSet cannot have null puts")
    requireNotNull(removes, "A CacheChangeSet cannot have null removes")

    ScalaCacheChangeSet(puts, removes, isCacheImage)
  }

  override def getCacheChangeSet(
    json: JsonNode,
    builder: JsonCacheModule
  ): CacheChangeSet = {
    
    requireNotNull(json, "A CacheChangeSet cannot be created from null json")
    requireNotNull(builder, "Cannot create objects for a CacheChangeSet from a null builder")
    
    ScalaCacheChangeSet(json)(builder)
  }

  override def getCache(
    cacheObjects: util.Set[_ <: CacheObject]
  ): Cache = {

    requireNotNull(cacheObjects, "A Cache cannot have null content")

    new ScalaCache(
      cacheObjects.asScala.foldLeft(Map[String, CacheObject]()) { (m, cacheObject) =>
        m + (cacheObject.getId -> cacheObject)
      })
  }

  override def getCacheChangeCalculator(
    cacheChangeSet: CacheChangeSet
  ): CacheChangeCalculator = {

    requireNotNull(cacheChangeSet, "A CacheChanger cannot have null content")
    require(!cacheChangeSet.isCacheImage, "A CacheChanger cannot use a cache image CacheChangeSet")

    new ScalaCacheChangeCalculator(cacheChangeSet)
  }

  override def getJsonCache(
    cacheId: String,
    subscriberBacklogLimit: Int,
    cache: Cache
  ): JsonCache = {

    requireNotNull(cacheId, "A JsonCache cannot have a null id")
    require(subscriberBacklogLimit > 0, "A JsonCache subscriberBacklogLimit must be > 0")
    requireNotNull(cache, "A JsonCache cannot be created with a null Cache")

    new ScalaJsonCache(cacheId, subscriberBacklogLimit, cache)
  }
}

object ScalaJsonCacheModule {
  
  val emptyContent: JsonNode = new ObjectMapper().createObjectNode()
  
  def requireNotNull(obj: Any, message: String): Unit = if(obj == null) throw new NullPointerException(message)
}
