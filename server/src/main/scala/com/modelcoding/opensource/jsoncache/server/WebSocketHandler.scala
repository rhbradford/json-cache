// Author: richard
// Date:   07 Jul 2017

package com.modelcoding.opensource.jsoncache.server

import grizzled.slf4j.Logging
import org.springframework.web.socket.{CloseStatus, TextMessage, WebSocketSession}
import org.springframework.web.socket.handler.TextWebSocketHandler

class WebSocketHandler extends TextWebSocketHandler with Logging {

  override def handleTextMessage(
    session: WebSocketSession,
    message: TextMessage
  ): Unit = {
    info(s"Message received: $message")
  }

  override def afterConnectionEstablished(
    session: WebSocketSession
  ): Unit = {
    info(s"Session connected: $session")
  }

  override def afterConnectionClosed(
    session: WebSocketSession,
    status: CloseStatus
  ): Unit = {
    info(s"Session disconnected: $session")
  }
}
