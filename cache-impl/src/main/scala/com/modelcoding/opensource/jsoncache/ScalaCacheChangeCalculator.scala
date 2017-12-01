// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

import scala.collection.JavaConverters._
import ScalaJsonCacheModule._

class ScalaCacheChangeCalculator(changeSet: CacheChangeSet) extends CacheFunctionInstance {

  override def getId: String = changeSet.getId

  override val getCode: CacheFunction = new ScalaCacheChangeCalculatorFunction(changeSet)
}

class ScalaCacheChangeCalculatorFunction(val getChangeSet: CacheChangeSet) extends CacheFunction {

  override def execute(
    cache: Cache
  ): CacheFunction.Result = {
    
    requireNotNull(cache, "Cannot calculate change against null cache")
    
    var nextCache: Cache = cache
    nextCache = getChangeSet.getPuts.iterator().asScala.foldLeft(nextCache) { (c, co) => c.put(co).getCache }
    nextCache = getChangeSet.getRemoves.iterator().asScala.foldLeft(nextCache) { (c, cr) => c.remove(cr).getCache }
    
    new ChangeResult(nextCache, getChangeSet)
  }
}

class ChangeResult(val getCache: Cache, val getChangeSet: CacheChangeSet) extends CacheFunction.Result
