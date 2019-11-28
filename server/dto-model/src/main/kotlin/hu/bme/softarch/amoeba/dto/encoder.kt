package hu.bme.softarch.amoeba.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

@Suppress("unused")
object DtoEncoder {
    private val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

    private fun <T> encodeAny(message: T?): String = mapper.writeValueAsString(message)

    private inline fun <reified T> decode(message: String): T = mapper.readValue(message, T::class.java)

    fun encode(message: WsServerMessage?) = encodeAny(message)

    fun encode(message: WsClientMessage?) = encodeAny(message)

    fun decodeServerMessage(message: String): WsServerMessage = decode(message)

    fun decodeClientMessage(message: String): WsClientMessage = decode(message)

}
