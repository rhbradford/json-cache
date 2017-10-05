// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode

case class ScalaCacheObject(getCacheObjectId: String, getCacheObjectType: String, getCacheObjectContent: JsonNode) 
  extends  CacheObject {

  require(getCacheObjectId != null, "A CacheObject cannot have a null cacheObjectId")
  require(getCacheObjectType != null, "A CacheObject cannot have a null cacheObjectType")
  require(getCacheObjectContent != null, "A CacheObject cannot have null cacheObjectContent")
}
