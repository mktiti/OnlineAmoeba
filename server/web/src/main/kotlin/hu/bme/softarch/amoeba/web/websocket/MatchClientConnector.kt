package hu.bme.softarch.amoeba.web.websocket

import com.fasterxml.jackson.core.JsonParseException
import hu.bme.softarch.amoeba.dto.WsClientMessage
import hu.bme.softarch.amoeba.dto.WsServerMessage.Error
import hu.bme.softarch.amoeba.web.api.InMemLobbyService
import hu.bme.softarch.amoeba.web.api.LobbyService
import hu.bme.softarch.amoeba.web.util.logger
import org.eclipse.jetty.websocket.api.CloseException
import java.util.concurrent.locks.ReentrantLock
import javax.websocket.*
import javax.websocket.server.PathParam
import javax.websocket.server.ServerEndpoint
import kotlin.concurrent.withLock

@Suppress("unused")
@ServerEndpoint("/game/{gameId}/{joinCode}", encoders = [WsMessageEncoder::class], decoders = [WsMessageDecoder::class])
class MatchClientConnector {

    companion object {
        private val matchLock = ReentrantLock()

        private val matches = mutableMapOf<Long, MatchController>()
    }

    private val log by logger()

    private val lobbyService: LobbyService = InMemLobbyService

    private fun getOrInitController(gameId: Long): MatchController? {
        return matches[gameId] ?: matchLock.withLock {
            matches[gameId] ?: MatchController(InMemLobbyService.getGame(gameId)
                    ?: return null).apply {
                matches[gameId] = this
            }
        }
    }

    @OnOpen
    fun onOpen(session: Session, @PathParam("gameId") gameId: Long, @PathParam("joinCode") joinCode: String) {
        fun error(message: String) {
            session.asyncRemote.sendObject(Error(message))
        }

        log.debug("WS client connected for game #$gameId")

        val controller = getOrInitController(gameId)
        if (controller == null) {
            error("Invalid game id")
            return
        }

        if (!controller.registerClient(joinCode, session.id) { session.asyncRemote.sendObject(it) }) {
            error("Invalid join code id")
        }
    }

    @OnMessage
    fun onMessage(@PathParam("gameId") gameId: Long, @PathParam("joinCode") joinCode: String, message: WsClientMessage) {
        matches[gameId]?.onMessage(joinCode, message)
    }

    @OnClose
    fun onClose(session: Session, @PathParam("gameId") gameId: Long, @PathParam("joinCode") joinCode: String) {
        matches[gameId]?.unregisterClient(joinCode, session.id)
    }

    @OnError
    fun onError(session: Session, error: Throwable) {
        fun error(message: String) {
            session.asyncRemote.sendObject(Error(message))
        }

        when (error) {
            is JsonParseException -> {
                log.debug("Invalid json message for match ws endpoint", error)
                error("Invalid JSON message received")
            }
            is CloseException -> log.debug("Close in ws match communication", error)
            else -> {
                log.info("Error in game ws: ", error)
                error("Error while processing message")
            }
        }
    }

}