// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.server.controller

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.modelcoding.opensource.jsoncache.{Cache, CacheChangeSet}
import com.modelcoding.opensource.jsoncache.server.JsonCacheServer
import grizzled.slf4j.Logging
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RestController}
import reactor.core.publisher.{Flux, Mono}

//@RestController
//class CacheController extends Logging {
//
//  @Autowired
//  private val cache: Cache = null
//  
//  private val objectMapper: ObjectMapper = new ObjectMapper()
//  
//  @PostMapping(Array("/data"))
//  def alterCache(@RequestBody alterations: Flux[String]): Mono[Void] = {
//    
//    alterations.subscribe(json => {
//      
//      info(s"Received:\n$json")    
//      
//      val jsonNode: JsonNode = objectMapper.readValue(json, classOf[ObjectNode])
//      
//      val cacheChangeSet: CacheChangeSet = JsonCacheServer.cacheModule.getCacheChangeSet(jsonNode)
//    })
//    
//    Mono.empty()
//  }
//}
