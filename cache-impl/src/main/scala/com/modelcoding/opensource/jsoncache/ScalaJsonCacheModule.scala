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

  override def getCacheRemove(
    cacheObjectId: String
  ): CacheRemove =
    getCacheRemove(cacheObjectId, ScalaJsonCacheModule.emptyContent)

  override def getCacheChangeSet(
    puts: util.Set[_ <: CacheObject],
    removes: util.Set[_ <: CacheRemove]
  ): CacheChangeSet = {

    require(puts != null, "A CacheChangeSet cannot have null puts")
    require(removes != null, "A CacheChangeSet cannot have null removes")

    ScalaCacheChangeSet(puts.asScala.toSet, removes.asScala.toSet)
  }

  override def getCacheImage(
    cacheObjects: util.Set[_ <: CacheObject]
  ): CacheImage = {
    
    require(cacheObjects != null, "A CacheImage cannot have null cacheObjects")
    
    ScalaCacheImage(cacheObjects.asScala.toSet)
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
    require(!cacheChangeSet.isInstanceOf[CacheImage], "A CacheChanger cannot use a CacheImage")

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

object ScalaJsonCacheModule {
  
  val emptyContent: JsonNode = new ObjectMapper().createObjectNode()
}
