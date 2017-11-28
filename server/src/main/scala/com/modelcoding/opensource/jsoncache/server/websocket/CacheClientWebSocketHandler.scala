// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.server.websocket

import grizzled.slf4j.Logging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.{WebSocketHandler, WebSocketSession}
import reactor.core.publisher.{Flux, Mono}

@Component
class CacheClientWebSocketHandler extends WebSocketHandler with Logging {

  override def handle(session: WebSocketSession): Mono[Void] = {

    session.receive().doFinally(sig => { info(s"It's gone away: $sig") }).map[String](_.getPayloadAsText).log().subscribe()
    
    session.send(
      Flux.just(Array(session.textMessage("Hello World")): _*).publish() // Need the .publish() on the end to keep the publisher going.... !!
    )
  }
}
