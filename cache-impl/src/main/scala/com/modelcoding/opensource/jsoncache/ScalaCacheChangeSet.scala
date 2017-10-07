// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache
import java.util
import java.util.Collections

class ScalaCacheChangeSet(puts: util.Set[_ <: CacheObject], removes: util.Set[_ <: CacheRemove]) extends CacheChangeSet {
  
  require(puts != null, "A CacheChangeSet cannot have null puts")
  require(removes != null, "A CacheChangeSet cannot have null removes")

  override def getPuts: util.Set[_ <: CacheObject] = Collections.unmodifiableSet(puts)
  override def getRemoves: util.Set[_ <: CacheRemove] = Collections.unmodifiableSet(removes)
}
