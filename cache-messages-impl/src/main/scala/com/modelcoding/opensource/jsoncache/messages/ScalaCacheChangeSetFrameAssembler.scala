// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages
import java.util

import com.fasterxml.jackson.databind.JsonNode
import com.modelcoding.opensource.jsoncache.{CacheMessage, CacheObject, CacheRemove, JsonCacheModule}
import com.modelcoding.opensource.jsoncache.messages.CacheChangeSetFrameAssembler.Receiver

import scala.collection.mutable
import scala.collection.JavaConverters._

class ScalaCacheChangeSetFrameAssembler(implicit cacheModule: JsonCacheModule) extends CacheChangeSetFrameAssembler {

  private var receiver: Receiver = _
  
  override def connect(
    receiver: Receiver
  ): Unit = {
    
    if(this.receiver != null)
      throw new IllegalStateException("CacheChangeSetFrameAssembler already connected")
    
    this.receiver = receiver
  }
  
  private val buffer: mutable.MutableList[CacheMessage] = mutable.MutableList()

  private trait State {
    def onCacheMessage(cacheMessage: CacheMessage): Unit
  }
  
  private var state: State = ExpectingStart()
  
  private def numPuts(): Int = {
    
    buffer.head.asInstanceOf[StartOfCacheChangeSet].getNumPuts
  }
  
  private def numRemoves(): Int = {
    
    buffer.head.asInstanceOf[StartOfCacheChangeSet].getNumRemoves
  }
  
  private def id(): String = {
    
    buffer.head.asInstanceOf[StartOfCacheChangeSet].getId
  }
  
  private case class ExpectingStart() extends State {

    override def onCacheMessage(cacheMessage: CacheMessage): Unit = {
      
      if(cacheMessage.isInstanceOf[StartOfCacheChangeSet])
        buffer += cacheMessage
      else
        throw new IllegalArgumentException(s"Expecting ${classOf[StartOfCacheChangeSet].getSimpleName}, but received $cacheMessage")
    
      if(numPuts() > 0)
        state = ExpectingPuts(1+numPuts())
      else if(numRemoves() > 0)
        state = ExpectingRemoves(1+numRemoves())
      else
        state = ExpectingEnd()
    }
  }
  
  private case class ExpectingPuts(transitionSize: Int) extends State {

    override def onCacheMessage(cacheMessage: CacheMessage): Unit = {
      
      if(cacheMessage.isInstanceOf[CacheObject])
        buffer += cacheMessage
      else
        throw new IllegalArgumentException(s"Expecting ${classOf[CacheObject].getSimpleName}, but received $cacheMessage")
      
      if(buffer.size == transitionSize) {
        if(numRemoves() > 0)
          state = ExpectingRemoves(transitionSize+numRemoves())
        else
          state = ExpectingEnd()
      }
    }
  }
  
  private case class ExpectingRemoves(transitionSize: Int) extends State {

    override def onCacheMessage(cacheMessage: CacheMessage): Unit = {
      
      if(cacheMessage.isInstanceOf[CacheRemove])
        buffer += cacheMessage
      else
        throw new IllegalArgumentException(s"Expecting ${classOf[CacheRemove].getSimpleName}, but received $cacheMessage")
      
      if(buffer.size == transitionSize) {
        state = ExpectingEnd()
      }
    }
  }
  
  private case class ExpectingEnd() extends State {

    override def onCacheMessage(cacheMessage: CacheMessage): Unit = {
      
      if(cacheMessage.isInstanceOf[EndOfCacheChangeSet])
        buffer += cacheMessage
      else
        throw new IllegalArgumentException(s"Expecting ${classOf[EndOfCacheChangeSet].getSimpleName}, but received $cacheMessage")

      val receivedId: String = cacheMessage.asInstanceOf[EndOfCacheChangeSet].getId
      val expectedId: String = id()
      
      if(expectedId != receivedId)    
        throw new IllegalArgumentException(s"Expecting ${classOf[EndOfCacheChangeSet].getSimpleName} with id of $expectedId, but found $receivedId in received $cacheMessage")
      
      val messages: util.List[CacheMessage] = buffer.toList.asJava
      
      receiver.onCacheChangeSetFrame(new ScalaCacheChangeSetFrameWrappingMessages(messages))
      buffer.clear()
      
      state = ExpectingStart()
    }
  }

  override def onCacheMessage(cacheMessage: CacheMessage): Unit = {
    
    if(receiver == null)
      throw new IllegalStateException("CacheChangeSetFrameAssembler not connected")
    
    state.onCacheMessage(cacheMessage)
  }

  override def getCacheMessage(
    json: JsonNode
  ): CacheMessage = {
    
    if(json.isObject) {
      
      val type_json: JsonNode = json.get("type")
      if(type_json != null)
        return cacheModule.getCacheObject(json)
      
      val frame_json: JsonNode = json.get("frame")
      if(frame_json != null && frame_json.isTextual) {
        
        if("start" == frame_json.asText())
          return ScalaStartOfCacheChangeSet(json)
        else
          return ScalaEndOfCacheChangeSet(json)
      }
      
      val isCacheImage_json: JsonNode = json.get("isCacheImage")
      if(isCacheImage_json != null)
        return cacheModule.getCacheChangeSet(json)
      
      return cacheModule.getCacheRemove(json)
    }
    
    throw new IllegalArgumentException(s"Cannot create CacheMessage from $json")
  }
}
