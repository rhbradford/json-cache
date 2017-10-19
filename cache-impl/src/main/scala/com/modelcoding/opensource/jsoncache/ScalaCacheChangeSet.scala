// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util
import scala.collection.JavaConverters._

case class ScalaCacheChangeSet(puts: Set[_ <: CacheObject], removes: Set[_ <: CacheRemove]) 
  extends CacheChangeSet {

  override def getPuts: util.Set[_ <: CacheObject] = puts.asJava 

  override def getRemoves: util.Set[_ <: CacheRemove] = removes.asJava
}
