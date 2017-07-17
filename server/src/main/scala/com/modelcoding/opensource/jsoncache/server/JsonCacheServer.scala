// Author: richard
// Date:   07 Jul 2017

package com.modelcoding.opensource.jsoncache.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.api.{WebSocketBehavior, WebSocketPolicy}
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.{EnableAutoConfiguration, SpringBootApplication}
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.{ViewControllerRegistry, WebMvcConfigurerAdapter}
import org.springframework.web.socket.config.annotation.{EnableWebSocket, WebSocketConfigurer, WebSocketHandlerRegistry}
import org.springframework.web.socket.server.HandshakeHandler
import org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy
import org.springframework.web.socket.server.standard.ServerEndpointExporter
import org.springframework.web.socket.server.support.DefaultHandshakeHandler

@EnableAutoConfiguration
@EnableWebSocket
@SpringBootApplication
class JsonCacheServer extends WebMvcConfigurerAdapter with WebSocketConfigurer {

  override def addViewControllers(
    registry: ViewControllerRegistry
  ): Unit = {
    registry.addViewController("/").setViewName("redirect:/assets/index.html")
  }

  override def registerWebSocketHandlers(
    registry: WebSocketHandlerRegistry
  ): Unit = {
    registry.addHandler(webSocketHandler, "/cache")
      .setHandshakeHandler(handshakeHandler)
      .setAllowedOrigins("*")  
  }
  
  @Bean
  def embeddedServletContainerFactory(): EmbeddedServletContainerFactory = {
    val factory = new JettyEmbeddedServletContainerFactory
    factory.addServerCustomizers((server: Server) => {
      server.getBean(classOf[WebAppContext]).setThrowUnavailableOnStartupException(true)
    })
    factory
  }
  
  @Bean
  def webSocketHandler = new WebSocketHandler
  
  @Bean
  def serverEndpointExporter = new ServerEndpointExporter 
  
  @Bean
  def handshakeHandler: HandshakeHandler = {
    val policy = new WebSocketPolicy(WebSocketBehavior.SERVER)
    policy.setInputBufferSize(8192)
    policy.setIdleTimeout(600000)
    
    new DefaultHandshakeHandler(new JettyRequestUpgradeStrategy(policy))
  }
}

object JsonCacheServer extends App {

  SpringApplication.run(classOf[JsonCacheServer])
}
