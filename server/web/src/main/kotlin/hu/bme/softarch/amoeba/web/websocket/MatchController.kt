package hu.bme.softarch.amoeba.web.websocket

import hu.bme.softarch.amoeba.dto.WsClientMessage
import hu.bme.softarch.amoeba.dto.WsServerMessage
import hu.bme.softarch.amoeba.dto.WsServerMessage.*
import hu.bme.softarch.amoeba.game.MapField
import hu.bme.softarch.amoeba.game.MutableField
import hu.bme.softarch.amoeba.game.Pos
import hu.bme.softarch.amoeba.game.Sign
import hu.bme.softarch.amoeba.web.api.FullGame
import hu.bme.softarch.amoeba.web.api.GameData
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

typealias OutChannel = (WsServerMessage) -> Unit

typealias ResultCallback = (id: Long, winner: Sign, rounds: Int) -> Unit

class MatchController(
        fullGame: FullGame,
        private val gameEndCallback: ResultCallback
) {

    enum class RegisterResult {
        INVALID_JOIN, MATCH_CLOSED, SUCCESS
    }

    private inner class ClientData(
            val joinCode: String
    ) {
        val outChannels = mutableMapOf<String, OutChannel>()
    }

    private val channelLock: ReadWriteLock = ReentrantReadWriteLock()

    private val placeCheck = AtomicBoolean()

    private val gameInfo = fullGame.info
    private val gameField: MutableField = MapField(
            toWin = fullGame.info.toWin,
            xs = fullGame.data.xTiles,
            os = fullGame.data.oTiles
    )

    private var round = fullGame.data.xTiles.size + fullGame.data.oTiles.size

    private var waitingFor: Sign? = if (fullGame.data.xTiles.size == fullGame.data.oTiles.size) Sign.X else Sign.O

    private val xData = ClientData(fullGame.info.hostCode)
    private val oData = ClientData(fullGame.info.clientCode)

    fun registerClient(joinCode: String, channelId: String, channel: OutChannel, ignoreClose: Boolean = false): RegisterResult {
        return channelLock.readLock().withLock {
            if (!ignoreClose && xData.outChannels.isEmpty() && oData.outChannels.isEmpty()) {
                RegisterResult.MATCH_CLOSED
            } else {
                val valid = onActor(joinCode, channelId) { player, _ ->
                    addChannel(player, channelId, channel)
                }
                if (valid) {
                    RegisterResult.SUCCESS
                } else  {
                    RegisterResult.INVALID_JOIN
                }
            }
        }
    }

    fun onMessage(joinCode: String, message: WsClientMessage): Boolean = onActor(joinCode, message, this::onMessage)

    fun unregisterClient(joinCode: String, channelId: String): Boolean {
        return channelLock.writeLock().withLock {
            onActor(joinCode, channelId, this::removeChannel)
            xData.outChannels.isEmpty() && oData.outChannels.isEmpty()
        }
    }

    private fun <T> onActor(joinCode: String, param: T, action: (Sign, T) -> Unit): Boolean {
        val player = mapSign(joinCode) ?: return false
        action(player, param)
        return true
    }

    private fun mapSign(joinCode: String): Sign? =  when (joinCode) {
        xData.joinCode -> Sign.X
        oData.joinCode -> Sign.O
        else -> null
    }

    private fun data(player: Sign): ClientData = when (player) {
        Sign.X -> xData
        Sign.O -> oData
    }

    private fun send(message: WsServerMessage) {
        send(Sign.X, message)
        send(Sign.O, message)
    }

    private fun send(player: Sign, message: WsServerMessage) {
        data(player).outChannels.forEach { (_, channel) ->
            channel(message)
        }
    }

    /**
     * Non thread-safe, only call with checks
     */
    private fun placeNew(player: Sign, position: Pos) {
        val next = waitingFor
        if (next == null) {
            send(player, Error("Game already finished"))
            return
        }

        if (next != player) {
            send(player, Error("Not your turn"))
        } else {
            if (gameField[position] == null) {
                round++
                val winRow = gameField.set(position, player)
                send(NewPoint(player, position))
                waitingFor = if (winRow != null) {
                    gameEndCallback(gameInfo.id, player, round)
                    send(GameResult(player, winRow))
                    null
                } else {
                    !next
                }
            } else {
                send(player, Error("Position '$position' already occupied, try again"))
            }
        }
    }

    private fun onMessage(player: Sign, message: WsClientMessage) {
        when (message) {
            is WsClientMessage.PutNew -> {

                while (!placeCheck.compareAndSet(false, true)) {
                    try {
                        placeNew(player, message.position)
                    } finally {
                        placeCheck.set(false)
                    }
                }

            }
            is WsClientMessage.PartScanRequest -> {
                send(player, PartScanResponse(
                        bounds = message.range,
                        xs = gameField.positionsOf(Sign.X, message.range),
                        os = gameField.positionsOf(Sign.O, message.range)
                ))
            }
            is WsClientMessage.FullScanRequest -> {
                send(player, FullScanResponse(xs = gameField.positionsOf(Sign.X), os = gameField.positionsOf(Sign.O)))
            }
        }
    }

    private fun addChannel(player: Sign, channelId: String, channel: OutChannel) {
        data(player).outChannels[channelId] = channel

        send(OpponentEvent("New client joined for $player"))
        channel(Info(player, waitingFor))
    }

    private fun removeChannel(player: Sign, channelId: String) {
        data(player).outChannels.remove(channelId)
        send(!player, OpponentEvent("Client closed for $player"))
    }

    fun getGame() = FullGame(
        info = gameInfo,
        data = GameData(
            xTiles = gameField.positionsOf(Sign.X).toList(),
            oTiles = gameField.positionsOf(Sign.O).toList()
        )
    )

}