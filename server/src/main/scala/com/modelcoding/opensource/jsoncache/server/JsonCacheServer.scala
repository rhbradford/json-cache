// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.server

import grizzled.slf4j.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.{EnableWebFlux, WebFluxConfigurer}
import org.springframework.web.reactive.function.server._
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.springframework.web.reactive.socket.{WebSocketHandler, WebSocketSession}
import org.springframework.web.server.{ServerWebExchange, WebFilter, WebFilterChain}
import reactor.core.publisher.{Flux, Mono}

@EnableWebFlux
@SpringBootApplication
class JsonCacheServer extends WebFluxConfigurer {
  @Configuration
  class WebSocketConfig {

    @Autowired
    private var webSocketHandler: WebSocketHandler = _

    @Bean
    def webSocketMapping(): HandlerMapping = {
      import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
      import org.springframework.web.reactive.socket.WebSocketHandler
      val map = new java.util.HashMap[String, WebSocketHandler]()
      map.put("/cache", webSocketHandler)

      val mapping = new SimpleUrlHandlerMapping
      mapping.setOrder(10)
      mapping.setUrlMap(map)

      mapping
    }

    @Bean
    def handlerAdapter(): WebSocketHandlerAdapter = {

      new WebSocketHandlerAdapter()
    }
  }

  @Component
  class MyWebSocketHandler extends WebSocketHandler with Logging {

    override def handle(
      session: WebSocketSession
    ): Mono[Void] = {

      session.receive().doFinally(sig => {
        info(s"It's gone away: $sig")
      }
      ).map[String](_.getPayloadAsText).log().subscribe()
      session.send(
        Flux.just(Array(session.textMessage("Hello World")): _*).publish() // Need the .publish() on the end to keep the publisher going.... !!
      )
    }
  }

  @Bean
  def router(): RouterFunction[ServerResponse] = RouterFunctions.resources("/**", new ClassPathResource("/static/"))

  @Configuration
  class TemporaryStaticResolver extends WebFilter {

    override def filter(
      exchange: ServerWebExchange,
      chain: WebFilterChain
    ): Mono[Void] = {

      if(exchange.getRequest.getURI.getPath == "/") {
        return chain.filter(exchange.mutate().request(exchange.getRequest.mutate().path("/assets/index.html").build()).build())
      }

      chain.filter(exchange)
    }
  }
}

object JsonCacheServer extends App {

  SpringApplication.run(classOf[JsonCacheServer])
}
