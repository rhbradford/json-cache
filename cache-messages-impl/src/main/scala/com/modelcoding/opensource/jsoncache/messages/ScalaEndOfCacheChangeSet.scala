// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import com.modelcoding.opensource.jsoncache.CacheChangeSet

class ScalaEndOfCacheChangeSet(val getId: String) extends EndOfCacheChangeSet {

  override def asJsonNode(): ObjectNode = {

    val json: ObjectNode = JsonNodeFactory.instance.objectNode()

    json.put("frame", "end")
    json.put("id", getId)

    json
  }

  override def equals(other: Any): Boolean = other match {
    case that: EndOfCacheChangeSet =>
      getId == that.getId
    case _                    => false
  }

  override val hashCode: Int = getId.hashCode
}

object ScalaEndOfCacheChangeSet {
  
  def apply(cacheChangeSet: CacheChangeSet): EndOfCacheChangeSet =
    new ScalaEndOfCacheChangeSet(cacheChangeSet.getId)
  
  def apply(json: JsonNode): EndOfCacheChangeSet = {
    
    if(json.isObject) {

      val frame_json: JsonNode = json.get("frame")
      if(frame_json != null && frame_json.isTextual && frame_json.asText() == "end") {
        
        val id_json: JsonNode = json.get("id")
        if(id_json != null && id_json.isTextual) {
          
          return new ScalaEndOfCacheChangeSet(id_json.asText())
        }
      }
    }

    throw new IllegalArgumentException(s"Unable to create EndOfCacheChangeSet from $json")
  }
}
