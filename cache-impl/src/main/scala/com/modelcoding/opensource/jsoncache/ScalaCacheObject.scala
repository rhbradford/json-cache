// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode

case class ScalaCacheObject(getId: String)(aType: String, someContent: JsonNode) 
  extends CacheObject {

  override def getType: String = aType
  override def getContent: JsonNode = someContent

  override def asCacheRemove(): CacheRemove = ScalaCacheRemove(getId)(ScalaJsonCacheModule.emptyContent)
}
