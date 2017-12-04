// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client

import java.util.function.Predicate

import akka.actor.ActorSystem
import com.modelcoding.opensource.jsoncache.client.ScalaJsonCacheClientModule._
import com.modelcoding.opensource.jsoncache.{CacheImageSender, CacheObject, JsonCacheModule}
import org.reactivestreams.Publisher

class ScalaJsonCacheClientModule(implicit val jsonCacheModule: JsonCacheModule, val actorSystem: ActorSystem) 
  extends JsonCacheClientModule {

  override def getCacheChangeSetProcessor(
    cacheObjectSelectors: Publisher[Predicate[CacheObject]]
  ): CacheChangeSetProcessor = {
    
    requireNotNull(cacheObjectSelectors, "Cannot create CacheChangeSetProcessor with null cacheObjectSelectors")
    
    new ScalaCacheChangeSetProcessor(cacheObjectSelectors)
  }

  override def getJsonCacheClient(
    id: String,
    input: CacheImageSender,
    cacheObjectSelector: CacheChangeSetProcessor,
    cacheObjectAuthorisor: CacheChangeSetProcessor
  ): JsonCacheClient = {
    
    requireNotNull(id, "Cannot create JsonCacheClient with null id")
    requireNotNull(input, "Cannot create JsonCacheClient with null input")
    requireNotNull(cacheObjectSelector, "Cannot create JsonCacheClient with null cacheObjectSelector")
    requireNotNull(cacheObjectAuthorisor, "Cannot create JsonCacheClient with null cacheObjectAuthorisor")
    
    new ScalaJsonCacheClient(id)(input, cacheObjectSelector, cacheObjectAuthorisor)
  }
}

object ScalaJsonCacheClientModule {
  
  def requireNotNull(obj: Any, message: String): Unit = if(obj == null) throw new NullPointerException(message)
}
