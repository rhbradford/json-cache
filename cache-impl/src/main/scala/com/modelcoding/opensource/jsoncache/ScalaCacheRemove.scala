// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode

case class ScalaCacheRemove(getId: String)(someContent: JsonNode) 
  extends CacheRemove {

  require(getId != null, "A CacheRemove cannot have a null id")
  require(getContent != null, "A CacheRemove cannot have null content")

  override def getContent: JsonNode = someContent
}
