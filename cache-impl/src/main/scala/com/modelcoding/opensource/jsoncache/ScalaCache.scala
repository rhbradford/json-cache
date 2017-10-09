// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util

import scala.collection.JavaConverters._

class ScalaCache(content: Map[String, _ <: CacheObject]) extends Cache {

  override def getObjects: util.Set[_ <: CacheObject] = content.values.toSet.asJava

  override def containsCacheObject(cacheObjectId: String): Boolean = content.contains(cacheObjectId)

  override def getCacheObject(
    cacheObjectId: String
  ): CacheObject = {
    
    require(containsCacheObject(cacheObjectId), "cacheObjectId not found in Cache")
    
    content(cacheObjectId)
  }

  override def put(
    cacheObject: CacheObject
  ): Cache =
    new ScalaCache(content + (cacheObject.getId -> cacheObject))

  override def remove(
    cacheRemove: CacheRemove
  ): Cache = {
    
    if(!containsCacheObject(cacheRemove.getId))
      this
    else
      new ScalaCache(content - cacheRemove.getId)
  }
}
