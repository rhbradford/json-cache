// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode

case class ScalaCacheRemove(getId: String)(someContent: JsonNode) 
  extends CacheRemove {

  override def getContent: JsonNode = someContent
}
