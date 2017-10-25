// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client
import com.modelcoding.opensource.jsoncache.client.ScalaJsonCacheClientModule.requireNotNull
import com.modelcoding.opensource.jsoncache.{CacheChangeSet, CacheImageSender}
import org.reactivestreams.Subscriber

class ScalaJsonCacheClient
(val getId: String)(input: CacheImageSender, selectors: CacheChangeSetProcessor, authorisors: CacheChangeSetProcessor) 
  extends JsonCacheClient {

  private var subscriber: Subscriber[_ >: CacheChangeSet] = _
  
  override def subscribe(
    subscriber: Subscriber[_ >: CacheChangeSet]
  ): Unit = {
    
    requireNotNull(subscriber, "Cannot subscribe to a JsonCacheClient with a null subscriber")
    
    this.synchronized {
     
      if(this.subscriber != null)
        throw new IllegalStateException("Cannot subscribe more than once to a JsonCacheClient")

      this.subscriber = subscriber
    }
    
    authorisors.connect(selectors)
    selectors.connect(input)
    authorisors.subscribe(subscriber)
  }
}
