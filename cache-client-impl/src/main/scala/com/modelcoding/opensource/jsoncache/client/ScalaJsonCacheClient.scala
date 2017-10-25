// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client
import com.modelcoding.opensource.jsoncache.client.ScalaJsonCacheClientModule.requireNotNull
import com.modelcoding.opensource.jsoncache.{CacheChangeSet, JsonCache}
import org.reactivestreams.Subscriber

class ScalaJsonCacheClient(val getJsonCache: JsonCache) extends JsonCacheClient {

  private var subscriber: Subscriber[_ >: CacheChangeSet] = _
  
  override def subscribe(
    subscriber: Subscriber[_ >: CacheChangeSet]
  ): Unit = {
    
    requireNotNull(subscriber, "Cannot subscribe to a ScalaJsonCacheClient with a null subscriber")
    if(this.subscriber != null)
      throw new IllegalStateException("Cannot subscribe more than once to a ScalaJsonCacheClient")

    this.subscriber = subscriber
    
    
  }
}
