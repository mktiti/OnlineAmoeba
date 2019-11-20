package hu.bme.softarch.amoeba.web.websocket

import com.fasterxml.jackson.core.JsonParseException
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
class MatchClientController {

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
        log.debug("WS client connected for game #$gameId")

        val controller = getOrInitController(gameId)
        if (controller == null) {
            session.asyncRemote.sendText("Invalid game id")
            return
        }

        controller.registerClient(joinCode, session.id) { session.asyncRemote.sendObject(it) }
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
        when (error) {
            is JsonParseException -> {
                log.debug("Invalid json message for match ws endpoint", error)
                session.asyncRemote.sendObject(WsServerMessage.Error("Invalid JSON message received"))
            }
            is CloseException -> log.debug("Close in ws match communication", error)
            else -> {
                log.info("Error in game ws: ", error)
                session.asyncRemote.sendObject(WsServerMessage.Error("Error while processing message"))
            }
        }
    }

}