// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import com.fasterxml.jackson.databind.JsonNode

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
}

object ScalaCacheRemove {
  
  def apply(id: String)(someContent: JsonNode): CacheRemove = new ScalaCacheRemove(id)(someContent)
}
