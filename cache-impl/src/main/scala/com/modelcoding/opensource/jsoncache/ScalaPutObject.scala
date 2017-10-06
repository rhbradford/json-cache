// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode

case class ScalaPutObject(getId: String, getType: String, getContent: JsonNode) 
  extends PutObject {

  require(getId != null, "A PutObject cannot have a null id")
  require(getType != null, "A PutObject cannot have a null type")
  require(getContent != null, "A PutObject cannot have null content")
}
