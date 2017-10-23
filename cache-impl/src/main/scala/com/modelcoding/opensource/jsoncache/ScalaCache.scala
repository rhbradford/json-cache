// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import scala.collection.JavaConverters._
import ScalaJsonCacheModule._

class ScalaCache(content: Map[String, _ <: CacheObject]) extends Cache {

  override def getImage: CacheChangeSet = 
    ScalaCacheChangeSet(content.values.toSet.asJava, ScalaCacheChangeSet.emptyRemoves, isCacheImage = true)

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
  ): Cache = {

    requireNotNull(cacheObject, "Cannot put null object into cache")
    
    new ScalaCache(content + (cacheObject.getId -> cacheObject))
  }

  override def remove(
    cacheRemove: CacheRemove
  ): Cache = {
    
    requireNotNull(cacheRemove, "Cannot remove from cache using null remove")
    
    if(!containsCacheObject(cacheRemove.getId))
      this
    else
      new ScalaCache(content - cacheRemove.getId)
  }
}
