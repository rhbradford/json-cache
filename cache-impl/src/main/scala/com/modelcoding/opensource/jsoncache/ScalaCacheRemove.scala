// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}

class ScalaCacheRemove(val getId: String)(someContent: JsonNode) 
  extends CacheRemove {

  override def getContent: JsonNode = someContent

  override def equals(other: Any): Boolean = other match {
    case that: CacheRemove => getId == that.getId
    case _ => false
  }

  override val hashCode: Int = {
    val state = Seq(getId)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  override def asJsonNode(): ObjectNode = {
    
    val json: ObjectNode = JsonNodeFactory.instance.objectNode()
    
    json.put("id", getId)
    json.set("content", someContent)
    
    json
  }
}

object ScalaCacheRemove {
  
  def apply(id: String)(someContent: JsonNode): CacheRemove = new ScalaCacheRemove(id)(someContent)
  
  def apply(json: JsonNode): CacheRemove = {
    
    if(json.isObject) {
      val id_json: JsonNode = json.get("id")
      if(id_json != null && id_json.isTextual) {
          val content: JsonNode = json.get("content")
          if(content != null)
            return apply(id_json.asText())(content)
      }
    }

    throw new IllegalArgumentException(s"Cannot create a CacheRemove from $json")
  }
}
