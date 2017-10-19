// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import java.util

import scala.collection.JavaConverters._

case class ScalaCacheImage(cacheObjects: Set[_ <: CacheObject]) 
  extends CacheImage {

  override def getPuts: util.Set[_ <: CacheObject] = cacheObjects.asJava 

  override def getRemoves: util.Set[_ <: CacheRemove] = ScalaCacheImage.emptyRemoves
}

object ScalaCacheImage {
  val emptyRemoves: util.Set[_ <: CacheRemove] = Set().asJava
}
