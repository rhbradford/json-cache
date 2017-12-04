// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages

import java.util

import scala.collection.JavaConverters._

import com.modelcoding.opensource.jsoncache.{CacheChangeSet, CacheMessage}

class ScalaCacheChangeSetFrame(val getCacheChangeSet: CacheChangeSet) extends CacheChangeSetFrame {

  override def getMessages: util.List[CacheMessage] = {
    
    var o: Seq[CacheMessage] = Vector[CacheMessage](ScalaStartOfCacheChangeSet(getCacheChangeSet))
    getCacheChangeSet.getPuts.asScala.foreach { p: CacheMessage => o = o :+ p }
    getCacheChangeSet.getRemoves.asScala.foreach { r: CacheMessage => o = o :+ r }
    o = o :+ ScalaEndOfCacheChangeSet(getCacheChangeSet)
    
    o.asJava
  }
}
