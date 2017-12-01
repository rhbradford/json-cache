// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import java.util
import java.util.Collections

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

  val emptyRemoves: util.Set[CacheRemove] = Collections.unmodifiableSet(new util.HashSet[CacheRemove]())
}

abstract class ScalaCacheChangeSet private[ScalaCacheChangeSet](
  val getId: String,
  val getPuts: util.Set[_ <: CacheObject],
  val getRemoves: util.Set[_ <: CacheRemove],
  val isCacheImage: Boolean
)
  extends CacheChangeSet {

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
