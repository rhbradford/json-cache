// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode

case class ScalaCacheObject(getId: String)(aType: String, someContent: JsonNode) 
  extends CacheObject {

  require(getId != null, "A CacheObject cannot have a null id")
  require(getType != null, "A CacheObject cannot have a null type")
  require(getContent != null, "A CacheObject cannot have null content")

  override def getType: String = aType
  override def getContent: JsonNode = someContent
}
