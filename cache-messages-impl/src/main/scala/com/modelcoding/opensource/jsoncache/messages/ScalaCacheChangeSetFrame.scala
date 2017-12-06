// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import java.util
import java.util.concurrent.atomic.AtomicReference
import java.util.stream
import java.util.stream.Collectors

import scala.collection.JavaConverters._
import com.modelcoding.opensource.jsoncache._

class ScalaCacheChangeSetFrameWrappingChangeSet(val getCacheChangeSet: CacheChangeSet) extends CacheChangeSetFrame {

  override def getMessages: util.List[CacheMessage] = {
    
    var o: Seq[CacheMessage] = Vector[CacheMessage](ScalaStartOfCacheChangeSet(getCacheChangeSet))
    getCacheChangeSet.getPuts.asScala.foreach { p: CacheMessage => o = o :+ p }
    getCacheChangeSet.getRemoves.asScala.foreach { r: CacheMessage => o = o :+ r }
    o = o :+ ScalaEndOfCacheChangeSet(getCacheChangeSet)
    
    o.asJava
  }
}

class ScalaCacheChangeSetFrameWrappingMessages(val getMessages: java.util.List[CacheMessage])
  (implicit cacheModule: JsonCacheModule) 
  extends CacheChangeSetFrame {

  private def changeSet(getMessages: util.List[CacheMessage]): CacheChangeSet = {
    
    val startOfCacheChangeSet: StartOfCacheChangeSet = getMessages.get(0).asInstanceOf[StartOfCacheChangeSet]
    
    val id: String = startOfCacheChangeSet.getId
    val isCacheImage: Boolean = startOfCacheChangeSet.isCacheImage
    val numPuts: Int = startOfCacheChangeSet.getNumPuts
    val numRemoves: Int = startOfCacheChangeSet.getNumRemoves

    val putsStream: stream.Stream[CacheObject] = getMessages.stream()
        .skip(1).limit(numPuts)
        .map(msg => msg.asInstanceOf[CacheObject])
      
    val puts: util.Set[CacheObject] = putsStream.collect(Collectors.toSet())
    
    val removesStream: stream.Stream[CacheRemove] = getMessages.stream()
        .skip(1+numPuts).limit(numRemoves)
        .map(msg => msg.asInstanceOf[CacheRemove])
      
    val removes: util.Set[CacheRemove] = removesStream.collect(Collectors.toSet())

    cacheModule.getCacheChangeSet(id, puts, removes, isCacheImage)    
  }
  
  override val getCacheChangeSet: CacheChangeSet = changeSet(getMessages)
}
