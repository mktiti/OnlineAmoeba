package hu.bme.softarch.amoeba.web.websocket

import com.fasterxml.jackson.core.JsonParseException
import hu.bme.softarch.amoeba.dto.WsClientMessage
import hu.bme.softarch.amoeba.dto.WsServerMessage
import hu.bme.softarch.amoeba.dto.WsServerMessage.Error
import hu.bme.softarch.amoeba.web.api.DbLobbyService
import hu.bme.softarch.amoeba.web.api.LobbyService
import hu.bme.softarch.amoeba.web.util.logger
import org.eclipse.jetty.websocket.api.CloseException
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.websocket.*
import javax.websocket.server.PathParam
import javax.websocket.server.ServerEndpoint
import kotlin.concurrent.withLock

@Suppress("unused")
@ServerEndpoint("/game/{gameId}/{joinCode}", encoders = [WsMessageEncoder::class], decoders = [WsMessageDecoder::class])
class MatchClientConnector @JvmOverloads constructor(
        private val lobbyService: LobbyService = DbLobbyService()
) {

    class MatchJoinException(message: String) : RuntimeException(message)

    private class ControllerHolder(
            val controller: MatchController
    ) {
        val lock: ReadWriteLock = ReentrantReadWriteLock()
    }

    companion object {
        private val matches = mutableMapOf<Long, MatchController>()

        private val matchLock: ReadWriteLock = ReentrantReadWriteLock()

        private val matchInitLock: Lock = ReentrantLock()
    }

    private val log by logger()

    @OnOpen
    fun onOpen(session: Session, @PathParam("gameId") gameId: Long, @PathParam("joinCode") joinCode: String) {
        log.debug("WS client connected for game #$gameId")

        fun error(message: String) {
            session.asyncRemote.sendObject(Error(message))
        }

        val channel: OutChannel = { session.asyncRemote.sendObject(it) }

        /** Null on invalid game, controller to is-newly-created otherwise */
        fun getOrInitController(): Pair<MatchController, Boolean>? {
            val controller = matches[gameId]
            return if (controller != null) {
                controller to false
            } else {
                matchInitLock.withLock {
                    val safeController = matches[gameId]
                    if (safeController != null) {
                        safeController to false
                    } else {
                        lobbyService.getGame(gameId)?.let {
                            val matchController = MatchController(it)
                            if (matchController.registerClient(joinCode, session.id, channel, ignoreClose = true) == MatchController.RegisterResult.INVALID_JOIN) {
                                throw MatchJoinException("Invalid join code")
                            } else {
                                matches[gameId] = matchController
                            }
                            matchController to true
                        }
                    }
                }
            }
        }

        try {
            do {
                val (controller, isNew) = getOrInitController() ?: throw MatchJoinException("Invalid game id")

                val result = if (isNew) {
                    break
                } else {
                    controller.registerClient(joinCode, session.id, channel).apply {
                        if (this == MatchController.RegisterResult.INVALID_JOIN) {
                            throw MatchJoinException("Invalid join code")
                        }
                    }
                }
            } while (result == MatchController.RegisterResult.MATCH_CLOSED)
        } catch (mje: MatchJoinException) {
            error(mje.message ?: "Invalid join parameters")
        }
    }

    @OnMessage
    fun onMessage(@PathParam("gameId") gameId: Long, @PathParam("joinCode") joinCode: String, message: WsClientMessage) {
        matches[gameId]?.onMessage(joinCode, message)
    }

    @OnClose
    fun onClose(session: Session, @PathParam("gameId") gameId: Long, @PathParam("joinCode") joinCode: String) {
        val controller = matches[gameId]
        if (controller != null) {
            if (controller.unregisterClient(joinCode, session.id)) {
                matches.remove(gameId)
                matchInitLock.withLock {
                    lobbyService.updateGame(controller.getGame())
                }
            }
        }
    }

    @OnError
    fun onError(session: Session?, error: Throwable) {
        if (session == null) {
            log.debug("Error while connecting to match ws endpoint", error)
            return
        }

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