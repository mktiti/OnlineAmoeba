package hu.bme.softarch.amoeba.web.websocket

import hu.bme.softarch.amoeba.dto.DtoEncoder
import hu.bme.softarch.amoeba.dto.WsClientMessage
import hu.bme.softarch.amoeba.dto.WsServerMessage
import javax.websocket.Decoder
import javax.websocket.Encoder
import javax.websocket.EndpointConfig

class WsMessageEncoder : Encoder.Text<WsServerMessage> {

    override fun init(config: EndpointConfig?) {}

    override fun encode(message: WsServerMessage?): String = DtoEncoder.encode(message)

    override fun destroy() {}

}

class WsMessageDecoder : Decoder.Text<WsClientMessage> {

    override fun init(config: EndpointConfig?) {}

    override fun willDecode(message: String?): Boolean = message != null

    override fun decode(message: String): WsClientMessage = DtoEncoder.decodeClientMessage(message)

    override fun destroy() {}

}