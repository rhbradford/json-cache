// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.server.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.modelcoding.opensource.jsoncache.Cache
import grizzled.slf4j.Logging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}
import reactor.core.publisher.Mono

@RestController
class CacheController extends Logging {

  @Autowired
  private val cache: Cache = null
  
  private val objectMapper: ObjectMapper = new ObjectMapper()
  
  @PostMapping(Array("/data"))
  def alterCache(@RequestBody json: String): Mono[Void] = {
    
    info(s"Received:\n$json")    
    
    Mono.empty()
  }
}
