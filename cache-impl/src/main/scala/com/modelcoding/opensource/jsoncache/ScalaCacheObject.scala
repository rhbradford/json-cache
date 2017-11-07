// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}

class ScalaCacheObject(val getId: String)(aType: String, someContent: JsonNode) 
  extends CacheObject {

  override def getType: String = aType
  override def getContent: JsonNode = someContent

  override def asUpdatedCacheObject(content: JsonNode): CacheObject = ScalaCacheObject(getId)(aType, content)

  override def asCacheRemove(): CacheRemove = ScalaCacheRemove(getId)

  override def asJsonNode(): ObjectNode = {
    
    val json: ObjectNode = JsonNodeFactory.instance.objectNode()
    
    json.put("id", getId)
    json.put("type", aType)
    json.set("content", someContent)
    
    json
  }

  override def equals(other: Any): Boolean = other match {
    case that: CacheObject =>
      getId == that.getId
    case _ => false
  }
  
  override val hashCode: Int = {
    val state = Seq(getId)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object ScalaCacheObject {
  
  def apply(id: String)(aType: String, someContent: JsonNode): CacheObject = new ScalaCacheObject(id)(aType, someContent)
  
  def apply(json: JsonNode): CacheObject = {
    
    if(json.isObject) {
      val id_json: JsonNode = json.get("id")
      if(id_json != null && id_json.isTextual) {
        val type_json: JsonNode = json.get("type")
        if(type_json != null && type_json.isTextual) {
          val content: JsonNode = json.get("content")
          if(content != null)
            return apply(id_json.asText())(type_json.asText(), content)
        }
      }
    }

    throw new IllegalArgumentException(s"Cannot create a CacheObject from $json")
  }
}
