// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import java.util.function.Predicate

import com.modelcoding.opensource.jsoncache.{CacheObject, JsonCache, JsonCacheModule}
import org.reactivestreams.Publisher

class ScalaJsonCacheClientModule(implicit val jsonCacheModule: JsonCacheModule) extends JsonCacheClientModule {

  override def getCacheChangeSetProcessor(
    cacheObjectSelectors: Publisher[Predicate[CacheObject]]
  ): CacheChangeSetProcessor = ???

  override def getControlledCacheChangeSetSource(
    jsonCache: JsonCache,
    cacheObjectSelector: CacheChangeSetProcessor,
    cacheObjectAuthorisor: CacheChangeSetProcessor
  ): JsonCacheClient = null
}
