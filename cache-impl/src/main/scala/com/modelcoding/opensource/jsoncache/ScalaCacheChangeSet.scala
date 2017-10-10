// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util
import java.util.Collections

object ScalaCacheChangeSet {
   def apply(
     puts: util.Set[_ <: CacheObject],
     removes: util.Set[_ <: CacheRemove]
   ): ScalaCacheChangeSet =
    new ScalaCacheChangeSet(Collections.unmodifiableSet(puts), Collections.unmodifiableSet(removes)) {
      
    }
}

abstract case class ScalaCacheChangeSet private[ScalaCacheChangeSet] (
  getPuts: util.Set[_ <: CacheObject], 
  getRemoves: util.Set[_ <: CacheRemove]
) 
  extends CacheChangeSet {
  
  def copy(
    getPuts: util.Set[_ <: CacheObject] = getPuts, 
    getRemoves: util.Set[_ <: CacheRemove] = getRemoves
  ) = ScalaCacheChangeSet.apply(getPuts, getRemoves)
}
