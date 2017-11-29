// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.server.web

import grizzled.slf4j.Logging
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.{HandlerFunction, RouterFunction, ServerRequest, ServerResponse}
import org.springframework.web.reactive.function.server.RouterFunctions._
import org.springframework.web.reactive.function.server.RequestPredicates._
import reactor.core.publisher.{Flux, Mono}

object ApplicationRoutes extends Logging {

  private def dataHandler: HandlerFunction[_] = (serverRequest: ServerRequest) => {
    
    val alterations: Mono[String] = serverRequest.bodyToMono(classOf[String])
    
    alterations.flatMap(json => {

      info(s"Received:\n$json")
      
      Mono.create()
      
      ServerResponse.ok().body(Mono.just(json), classOf[String])
    })
  }
  
  def routes(): RouterFunction[_] = {
    nest(
      accept(MediaType.APPLICATION_JSON),
      route(POST("/data"), dataHandler)
    )
  }
}
