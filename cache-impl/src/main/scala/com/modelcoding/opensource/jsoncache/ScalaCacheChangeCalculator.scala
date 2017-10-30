// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import scala.collection.JavaConverters._
import ScalaJsonCacheModule._

class ScalaCacheChangeCalculator(val getChangeSet: CacheChangeSet) extends CacheFunction {

  override def execute(
    cache: Cache
  ): CacheFunction.Result = {
    
    requireNotNull(cache, "Cannot calculate change against null cache")
    
    var nextCache: Cache = cache
    nextCache = getChangeSet.getPuts.iterator().asScala.foldLeft(nextCache) { (c, co) => c.put(co) }
    nextCache = getChangeSet.getRemoves.iterator().asScala.foldLeft(nextCache) { (c, cr) => c.remove(cr) }
    
    new ChangeResult(nextCache, getChangeSet)
  }
}

class ChangeResult(val getCache: Cache, val getChangeSet: CacheChangeSet) extends CacheFunction.Result
