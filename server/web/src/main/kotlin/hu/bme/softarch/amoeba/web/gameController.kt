package hu.bme.softarch.amoeba.web

import hu.bme.softarch.amoeba.game.MapField
import hu.bme.softarch.amoeba.game.MutableField
import javax.websocket.*
import javax.websocket.server.PathParam
import javax.websocket.server.ServerEndpoint

interface ClientProxy {

    fun onClose(channel: (String) -> Unit)

    fun onMessage(message: String)

}

class MatchController {

    private inner class Proxy(private val isX: Boolean) : ClientProxy {
        override fun onClose(channel: (String) -> Unit) {
            removeChannel(isX, channel)
        }

        override fun onMessage(message: String) {
            onMessage(isX, message)
        }
    }

    private inner class ClientData(
            isX: Boolean,
            val joinCode: String
    ) {
        val proxy: ClientProxy by lazy { Proxy(isX) }

        val channels = mutableListOf<(String) -> Unit>()
    }

    private val game: MutableField by lazy { MapField(5) }

    private val xData = ClientData(true, "123")
    private val oData = ClientData(false, "abc")

    fun proxyForJoinCode(joinCode: String, channel: (String) -> Unit): ClientProxy? {
        val isX = when (joinCode) {
            xData.joinCode -> true
            oData.joinCode -> false
            else -> return null
        }

        addChannel(isX, channel)
        return data(isX).proxy
    }

    private fun data(isX: Boolean): ClientData = if (isX) xData else oData

    private fun send(toX: Boolean, message: String) {
        data(toX).channels.forEach { channel ->
            channel(message)
        }
    }

    private fun onMessage(fromX: Boolean, message: String) {
        send(!fromX, "Opponent message: '$message'")
    }

    private fun addChannel(fromX: Boolean, channel: (String) -> Unit) {
        data(fromX).channels += channel
        send(!fromX, "New client joined for ${if (fromX) 'X' else 'O'}")
    }

    private fun removeChannel(fromX: Boolean, channel: (String) -> Unit) {
        data(fromX).channels -= channel
        send(!fromX, "Client closed for ${if (fromX) 'X' else 'O'}")
    }

}

@ServerEndpoint("/game/{joinCode}")
open class MatchClientController {

    companion object {
        private val matches = listOf(MatchController())
    }

    private val log by logger()

    private lateinit var proxy: ClientProxy

    private lateinit var remote: RemoteEndpoint.Basic

    private lateinit var channel: (String) -> Unit

    @OnOpen
    fun onOpen(session: Session, @PathParam("joinCode") joinCode: String) {
        log.info("WS client connected")
        remote = session.basicRemote
        channel = remote::sendText
        proxy = matches.map { it.proxyForJoinCode(joinCode, channel) }.first() ?: return
    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
        proxy.onMessage(message)
    }

    @OnClose
    fun onClose(session: Session) {
        proxy.onClose(channel)
    }

}