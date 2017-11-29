// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.server

import akka.actor.ActorSystem
import com.modelcoding.opensource.jsoncache.{Cache, JsonCacheModule, ScalaJsonCacheModule}
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.config.{EnableWebFlux, WebFluxConfigurer}
import org.springframework.web.reactive.function.server._
import org.springframework.web.server.{ServerWebExchange, WebFilter, WebFilterChain}
import reactor.core.publisher.Mono

import scala.collection.JavaConverters._

@EnableWebFlux
@SpringBootApplication
class JsonCacheServer extends WebFluxConfigurer {

  import JsonCacheServer._
  
  @Bean
  def jsonCache: Cache = {

    cacheModule.getCache(Set().asJava)
  }
  
  @Configuration
  class TemporaryStaticResolver extends WebFilter {

    @Bean
    def router(): RouterFunction[ServerResponse] = RouterFunctions.resources("/**", new ClassPathResource("/static/"))

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

object JsonCacheServer {
  
  implicit val actorSystem: ActorSystem = ActorSystem("JsonCacheServer")
  val cacheModule: JsonCacheModule = new ScalaJsonCacheModule()
  
  def main(args: Array[String]) {

    SpringApplication.run(classOf[JsonCacheServer])
  }
}
