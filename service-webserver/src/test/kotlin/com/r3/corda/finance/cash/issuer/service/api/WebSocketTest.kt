package com.r3.corda.finance.cash.issuer.service.api

import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import java.util.*


class MyStompSessionHandler : StompSessionHandlerAdapter() {
    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        session.subscribe("/nostro-transactions", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type {
                return String::class.java
            }

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                println("received websockets nostro-transactions: $payload")
            }
        })
        session.subscribe("/node-transactions", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type {
                return String::class.java
            }

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                println("received websockets node-transactions: $payload")
            }
        })
    }
}

fun monitorExit() {
    val scanner = Scanner(System.`in`)
    try {
        while (true) {
            val line = scanner.nextLine()
            if ("exit" == line) {
                break
            }
        }
    } catch (ex: Exception) {

    } finally {
        scanner.close()
    }
}

fun main(args: Array<String>) {
    val taskScheduler = ThreadPoolTaskScheduler()
    taskScheduler.afterPropertiesSet()

    val transports = ArrayList<Transport>(1)
    transports.add(WebSocketTransport(StandardWebSocketClient()))
   // transports.add(JettyXhrTransport(HttpClient())

    val transport = SockJsClient(transports)
    val stompClient = WebSocketStompClient(transport)

    stompClient.messageConverter = StringMessageConverter()
    stompClient.taskScheduler = taskScheduler
    stompClient.defaultHeartbeat = longArrayOf(0, 0)

    val url = "ws://127.0.0.1:10015/stomp"
    val sessionHandler = MyStompSessionHandler()
    stompClient.connect(url, sessionHandler)
    //block and monitor exit action
    monitorExit()
}