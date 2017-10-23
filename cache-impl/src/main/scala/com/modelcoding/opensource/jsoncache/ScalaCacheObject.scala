// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode

class ScalaCacheObject(val getId: String)(aType: String, someContent: JsonNode) 
  extends CacheObject {

  override def getType: String = aType
  override def getContent: JsonNode = someContent

  override def asCacheRemove(): CacheRemove = ScalaCacheRemove(getId)(ScalaJsonCacheModule.emptyContent)

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
}
