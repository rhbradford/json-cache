// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.server.config

import com.modelcoding.opensource.jsoncache.server.web.ApplicationRoutes
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.web.reactive.function.server.RouterFunction

@Configuration
class WebConfig {

  @Bean
  def routerFunction(): RouterFunction[_] = ApplicationRoutes.routes() 
}
