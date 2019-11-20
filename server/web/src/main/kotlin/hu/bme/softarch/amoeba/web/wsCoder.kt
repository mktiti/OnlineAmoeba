package hu.bme.softarch.amoeba.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import javax.websocket.Decoder
import javax.websocket.Encoder
import javax.websocket.EndpointConfig

private object MapperHolder {
    val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
}

class WsMessageEncoder : Encoder.Text<WsServerMessage> {

    override fun init(config: EndpointConfig?) {}

    override fun encode(message: WsServerMessage?): String = MapperHolder.mapper.writeValueAsString(message)

    override fun destroy() {}

}

class WsMessageDecoder : Decoder.Text<WsClientMessage> {

    override fun init(config: EndpointConfig?) {}

    override fun willDecode(message: String?): Boolean = message != null

    override fun decode(message: String): WsClientMessage = MapperHolder.mapper.readValue(message, WsClientMessage::class.java)

    override fun destroy() {}

}