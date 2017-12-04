// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import com.modelcoding.opensource.jsoncache.CacheChangeSet

class ScalaStartOfCacheChangeSet(val getId: String, val isCacheImage: Boolean, val getNumPuts: Int, val getNumRemoves: Int) extends StartOfCacheChangeSet {

  override def asJsonNode(): ObjectNode = {

    val json: ObjectNode = JsonNodeFactory.instance.objectNode()

    json.put("frame", "start")
    json.put("id", getId)
    json.put("isCacheImage", isCacheImage)
    json.put("numPuts", getNumPuts)
    json.put("numRemoves", getNumRemoves)

    json
  }

  override def equals(other: Any): Boolean = other match {
    case that: StartOfCacheChangeSet =>
      getId == that.getId &&
      getNumPuts == that.getNumPuts &&
      getNumRemoves == that.getNumRemoves &&
      isCacheImage == that.isCacheImage
    case _                    => false
  }

  override val hashCode: Int = {
    val state = Seq(getNumPuts, getNumRemoves, isCacheImage, getId)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object ScalaStartOfCacheChangeSet {
  
  def apply(cacheChangeSet: CacheChangeSet): StartOfCacheChangeSet =
    new ScalaStartOfCacheChangeSet(
      cacheChangeSet.getId, 
      cacheChangeSet.isCacheImage, 
      cacheChangeSet.getPuts.size(), 
      cacheChangeSet.getRemoves.size()
    )
  
  def apply(json: JsonNode): StartOfCacheChangeSet = {
    
    if(json.isObject) {

      val frame_json: JsonNode = json.get("frame")
      if(frame_json != null && frame_json.isTextual && frame_json.asText() == "start") {
        
        val id_json: JsonNode = json.get("id")
        if(id_json != null && id_json.isTextual) {
          
          val isCacheImage_json: JsonNode = json.get("isCacheImage")
          if(isCacheImage_json != null && isCacheImage_json.isBoolean) {
            
            val puts_json: JsonNode = json.get("numPuts")
            if(puts_json != null && puts_json.isNumber) {
              
              val removes_json: JsonNode = json.get("numRemoves")
              if(removes_json != null && removes_json.isNumber) {
          
                return new ScalaStartOfCacheChangeSet(
                  id_json.asText(),
                  isCacheImage_json.asBoolean(),
                  puts_json.asInt(),
                  removes_json.asInt()
                )
              }              
            }
          }
        }
      }
    }

    throw new IllegalArgumentException(s"Unable to create StartOfCacheChangeSet from $json")
  }
}
