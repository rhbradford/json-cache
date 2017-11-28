// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.server.websocket

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig {

  @Autowired
  private val webSocketHandler: WebSocketHandler = null

  @Bean
  def webSocketMapping: HandlerMapping = {
    
    val map = new java.util.HashMap[String, WebSocketHandler]()
    map.put("/cache", webSocketHandler)

    val mapping = new SimpleUrlHandlerMapping
    mapping.setOrder(10)
    mapping.setUrlMap(map)

    mapping
  }

  @Bean
  def handlerAdapter: WebSocketHandlerAdapter = new WebSocketHandlerAdapter()
}
