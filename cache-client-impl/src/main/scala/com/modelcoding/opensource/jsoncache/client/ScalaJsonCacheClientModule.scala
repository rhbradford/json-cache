// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import java.util.function.Predicate

import akka.actor.ActorSystem
import com.modelcoding.opensource.jsoncache.{CacheObject, JsonCache, JsonCacheModule}
import org.reactivestreams.Publisher
import ScalaJsonCacheClientModule._

class ScalaJsonCacheClientModule(implicit val jsonCacheModule: JsonCacheModule, val actorSystem: ActorSystem) 
  extends JsonCacheClientModule {

  override def getCacheChangeSetProcessor(
    cacheObjectSelectors: Publisher[Predicate[CacheObject]]
  ): CacheChangeSetProcessor = {
    
    requireNotNull(cacheObjectSelectors, "Cannot create CacheChangeSetProcessor with null cacheObjectSelectors")
    
    new ScalaCacheChangeSetProcessor(cacheObjectSelectors)
  }

  override def getControlledCacheChangeSetSource(
    jsonCache: JsonCache,
    cacheObjectSelector: CacheChangeSetProcessor,
    cacheObjectAuthorisor: CacheChangeSetProcessor
  ): JsonCacheClient = null
}

object ScalaJsonCacheClientModule {
  
  def requireNotNull(obj: Any, message: String): Unit = if(obj == null) throw new NullPointerException(message)
}
