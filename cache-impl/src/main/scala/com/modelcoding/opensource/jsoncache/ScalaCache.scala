// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util.UUID

import scala.collection.JavaConverters._
import ScalaJsonCacheModule._
import com.modelcoding.opensource.jsoncache.Cache.{PutAction, PutResult, RemoveAction, RemoveResult}

class ScalaCache(content: Map[String, _ <: CacheObject]) extends Cache {

  override def getImage: CacheChangeSet = 
    ScalaCacheChangeSet(UUID.randomUUID.toString, content.values.toSet.asJava, ScalaCacheChangeSet.emptyRemoves, isCacheImage = true)

  override def containsCacheObject(cacheObjectId: String): Boolean = {
    
    requireNotNull(cacheObjectId, "Cannot check cache for object with null id")
    
    content.contains(cacheObjectId)
  }

  override def getCacheObject(
    cacheObjectId: String
  ): CacheObject = {
    
    requireNotNull(cacheObjectId, "Cannot get object from cache with null id")
    require(containsCacheObject(cacheObjectId), "cacheObjectId not found in Cache")
    
    content(cacheObjectId)
  }

  override def put(
    cacheObject: CacheObject
  ): PutResult = {

    requireNotNull(cacheObject, "Cannot put null object into cache")
    
    val action: PutAction = if(content.contains(cacheObject.getId)) PutAction.REPLACED else PutAction.ADDED
    
    new PutResult {
      
      override def getCache: Cache = new ScalaCache(content + (cacheObject.getId -> cacheObject))

      override val getAction: PutAction = action
    }
  }

  override def remove(
    cacheRemove: CacheRemove
  ): RemoveResult = {
    
    requireNotNull(cacheRemove, "Cannot remove from cache using null remove")
    
    if(!containsCacheObject(cacheRemove.getId))
      new RemoveResult {
        
        override def getCache: Cache = ScalaCache.this

        override def getAction: Cache.RemoveAction = RemoveAction.NO_CHANGE
      }
    else
      new RemoveResult {
        
        override def getCache: Cache = new ScalaCache(content - cacheRemove.getId)

        override def getAction: RemoveAction = RemoveAction.REMOVED
      }
  }
}
