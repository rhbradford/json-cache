// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages
import akka.actor.ActorSystem
import com.fasterxml.jackson.databind.JsonNode
import com.modelcoding.opensource.jsoncache.{CacheChangeSet, JsonCacheModule}

class ScalaJsonCacheMessagesModule(implicit val jsonCacheModule: JsonCacheModule, val actorSystem: ActorSystem) 
  extends JsonCacheMessagesModule {

  import ScalaJsonCacheMessagesModule.requireNotNull
  
  override def getStartOfCacheChangeSet(
    cacheChangeSet: CacheChangeSet
  ): StartOfCacheChangeSet = {
    
    requireNotNull(cacheChangeSet, "Cannot create StartOfCacheChangeSet from null cacheChangeSet")
    
    ScalaStartOfCacheChangeSet(cacheChangeSet)
  }

  override def getStartOfCacheChangeSet(
    json: JsonNode
  ): StartOfCacheChangeSet = {
    
    requireNotNull(json, "Cannot create StartOfCacheChangeSet from null json")
    
    ScalaStartOfCacheChangeSet(json)
  }

  override def getEndOfCacheChangeSet(
    cacheChangeSet: CacheChangeSet
  ): EndOfCacheChangeSet = {
    
    requireNotNull(cacheChangeSet, "Cannot create EndOfCacheChangeSet from null cacheChangeSet")
    
    ScalaEndOfCacheChangeSet(cacheChangeSet)
  }

  override def getEndOfCacheChangeSet(
    json: JsonNode
  ): EndOfCacheChangeSet = {
    
    requireNotNull(json, "Cannot create EndOfCacheChangeSet from null json")
    
    ScalaEndOfCacheChangeSet(json)
  }

  override def getCacheChangeSetFrame(
    cacheChangeSet: CacheChangeSet
  ): CacheChangeSetFrame = {
    
    requireNotNull(cacheChangeSet, "Cannot create CacheChangeSetFrame from null cacheChangeSet")
    
    new ScalaCacheChangeSetFrameWrappingChangeSet(cacheChangeSet)
  }

  override def getCacheChangeSetOutputStream: CacheChangeSetOutputStream = new ScalaCacheChangeSetOutputStream()

  override def getCacheChangeSetFrameAssembler: CacheChangeSetFrameAssembler = new ScalaCacheChangeSetFrameAssembler()
  
  override def getCacheChangeSetInputStream(
    frameAssembler: CacheChangeSetFrameAssembler
  ): CacheChangeSetInputStream = {
    
    requireNotNull(frameAssembler, "Cannot create CacheChangeSetInputStream from null frameAssembler")
    
    new ScalaCacheChangeSetInputStream(frameAssembler)
  }
}

object ScalaJsonCacheMessagesModule {
  
  def requireNotNull(obj: Any, message: String): Unit = if(obj == null) throw new NullPointerException(message)
}
