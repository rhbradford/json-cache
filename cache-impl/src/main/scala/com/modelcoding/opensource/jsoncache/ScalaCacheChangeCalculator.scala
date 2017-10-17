// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import scala.collection.JavaConverters._

class ScalaCacheChangeCalculator(val getChangeSet: CacheChangeSet) extends CacheChangeCalculator {

  override def calculateChange(
    cache: Cache
  ): CacheChangeCalculator.ChangeResult = {
    
    var nextCache: Cache = cache
    nextCache = getChangeSet.getPuts.iterator().asScala.foldLeft(nextCache) { (c, co) => c.put(co) }
    nextCache = getChangeSet.getRemoves.iterator().asScala.foldLeft(nextCache) { (c, cr) => c.remove(cr) }
    
    new ScalaChangeResult(nextCache, getChangeSet)
  }
}

class ScalaChangeResult(val getCache: Cache, val getChangeSet: CacheChangeSet) extends CacheChangeCalculator.ChangeResult
