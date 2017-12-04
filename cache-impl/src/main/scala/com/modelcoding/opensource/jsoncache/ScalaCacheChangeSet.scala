// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import java.util
import java.util.Collections

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory, ObjectNode}

object ScalaCacheChangeSet {

  def apply(
    id: String,
    puts: util.Set[_ <: CacheObject],
    removes: util.Set[_ <: CacheRemove],
    isCacheImage: Boolean
  ): CacheChangeSet =
    new ScalaCacheChangeSet(
      id,
      Collections.unmodifiableSet(new util.HashSet[CacheObject](puts)),
      Collections.unmodifiableSet(new util.HashSet[CacheRemove](removes)),
      isCacheImage
    ) {}

  def apply(json: JsonNode): CacheChangeSet = {

    if(json.isObject) {
      val id_json: JsonNode = json.get("id")
      if(id_json != null && id_json.isTextual) {
        val isCacheImage_json: JsonNode = json.get("isCacheImage")
        if(isCacheImage_json != null && isCacheImage_json.isBoolean) {
          val puts_json: JsonNode = json.get("puts")
          if(puts_json != null && puts_json.isArray) {
            val removes_json: JsonNode = json.get("removes")
            if(removes_json != null && removes_json.isArray) {
              val puts: util.Set[CacheObject] = new util.HashSet[CacheObject]()
              puts_json.elements().forEachRemaining { j =>
                puts.add(ScalaCacheObject(j))
              }

              val removes: util.Set[CacheRemove] = new util.HashSet[CacheRemove]()
              removes_json.elements().forEachRemaining { j =>
                removes.add(ScalaCacheRemove(j))
              }

              return apply(id_json.asText(), puts, removes, isCacheImage_json.asBoolean())
            }
          }
        }
      }
    }

    throw new IllegalArgumentException(s"Unable to create CacheChangeSet from $json")
  }

  val emptyRemoves: util.Set[CacheRemove] = Collections.unmodifiableSet(new util.HashSet[CacheRemove]())
}

abstract class ScalaCacheChangeSet private[ScalaCacheChangeSet](
  val getId: String,
  val getPuts: util.Set[_ <: CacheObject],
  val getRemoves: util.Set[_ <: CacheRemove],
  val isCacheImage: Boolean
)
  extends CacheChangeSet {

  override def asJsonNode(): ObjectNode = {

    val json: ObjectNode = JsonNodeFactory.instance.objectNode()

    json.put("id", getId)
    
    json.put("isCacheImage", isCacheImage)

    val putsArray: ArrayNode = JsonNodeFactory.instance.arrayNode(getPuts.size())
    getPuts.forEach { p => putsArray.add(p.asJsonNode()) }
    json.set("puts", putsArray)

    val removesArray: ArrayNode = JsonNodeFactory.instance.arrayNode(getPuts.size())
    getRemoves.forEach { r => removesArray.add(r.asJsonNode()) }
    json.set("removes", removesArray)

    json
  }

  override def equals(other: Any): Boolean = other match {
    case that: CacheChangeSet =>
      getPuts == that.getPuts &&
      getRemoves == that.getRemoves &&
      isCacheImage == that.isCacheImage
    case _                    => false
  }

  override val hashCode: Int = {
    val state = Seq(getPuts, getRemoves, isCacheImage)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
