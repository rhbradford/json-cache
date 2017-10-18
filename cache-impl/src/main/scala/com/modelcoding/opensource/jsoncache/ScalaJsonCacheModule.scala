// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import java.util

import akka.actor.ActorSystem
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.collection.JavaConverters._

class ScalaJsonCacheModule(implicit val actorSystem: ActorSystem) extends JsonCacheModule {

  override def getCacheObject(
    cacheObjectId: String,
    cacheObjectType: String,
    cacheObjectContent: JsonNode
  ): CacheObject = {

    require(cacheObjectId != null, "A CacheObject cannot have a null id")
    require(cacheObjectType != null, "A CacheObject cannot have a null type")
    require(cacheObjectContent != null, "A CacheObject cannot have null content")

    ScalaCacheObject(cacheObjectId)(cacheObjectType, cacheObjectContent)
  }

  override def getCacheRemove(
    cacheObjectId: String,
    cacheRemoveContent: JsonNode
  ): CacheRemove = {

    require(cacheObjectId != null, "A CacheRemove cannot have a null id")
    require(cacheRemoveContent != null, "A CacheRemove cannot have null content")

    ScalaCacheRemove(cacheObjectId)(cacheRemoveContent)
  }

  private val emptyContent: JsonNode = new ObjectMapper().createObjectNode()

  override def getCacheRemove(
    cacheObjectId: String
  ): CacheRemove =
    getCacheRemove(cacheObjectId, emptyContent)

  override def getCacheChangeSet(
    puts: util.Set[_ <: CacheObject],
    removes: util.Set[_ <: CacheRemove],
    isInitial: Boolean
  ): CacheChangeSet = {

    require(puts != null, "A CacheChangeSet cannot have null puts")
    require(removes != null, "A CacheChangeSet cannot have null removes")
    require(!isInitial || (isInitial && removes.isEmpty), "A CacheChangeSet marked as a cache image cannot contain removes")

    ScalaCacheChangeSet(puts.asScala.toSet, removes.asScala.toSet, isInitial)
  }

  override def getCache(
    cacheObjects: util.Set[_ <: CacheObject]
  ): Cache = {

    require(cacheObjects != null, "A Cache cannot have null content")

    new ScalaCache(
      cacheObjects.asScala.foldLeft(Map[String, CacheObject]()) { (m, cacheObject) =>
        m + (cacheObject.getId -> cacheObject)
      })
  }

  override def getCacheChangeCalculator(
    cacheChangeSet: CacheChangeSet
  ): CacheChangeCalculator = {

    require(cacheChangeSet != null, "A CacheChanger cannot have null content")
    require(!cacheChangeSet.isCacheImage, "The CacheChangeSet for a CacheChanger cannot be a cache image")

    new ScalaCacheChangeCalculator(cacheChangeSet)
  }

  override def getJsonCache(
    cacheId: String,
    subscriberBacklogLimit: Int,
    cache: Cache
  ): JsonCache = {

    require(cacheId != null, "A JsonCache cannot have a null id")
    require(subscriberBacklogLimit > 0, "A JsonCache subscriberBacklogLimit must be > 0")
    require(cache != null, "A JsonCache cannot be created with a null Cache")

    new ScalaJsonCache(cacheId, subscriberBacklogLimit, cache)
  }
}
