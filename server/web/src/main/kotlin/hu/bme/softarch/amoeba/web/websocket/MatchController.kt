package hu.bme.softarch.amoeba.web.websocket

import hu.bme.softarch.amoeba.game.MapField
import hu.bme.softarch.amoeba.game.MutableField
import hu.bme.softarch.amoeba.game.Sign
import hu.bme.softarch.amoeba.web.api.FullGame
import hu.bme.softarch.amoeba.web.websocket.WsServerMessage.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MatchController(fullGame: FullGame) {

    private inner class ClientData(
            val joinCode: String
    ) {
        val outChannels = mutableMapOf<String, (WsServerMessage) -> Unit>()
    }

    private val placeLock = ReentrantLock()

    private val gameField: MutableField = MapField(
            toWin = fullGame.info.toWin,
            xs = fullGame.data.xTiles,
            os = fullGame.data.oTiles
    )

    private var waitingFor: Sign? = if (fullGame.data.xTiles.size == fullGame.data.oTiles.size) Sign.X else Sign.O

    private val xData = ClientData(fullGame.info.xPass)
    private val oData = ClientData(fullGame.info.oPass)

    fun registerClient(joinCode: String, channelId: String, channel: (WsServerMessage) -> Unit): Boolean = onActor(joinCode, channelId) { player, _ ->
        addChannel(player, channelId, channel)
    }

    fun onMessage(joinCode: String, message: WsClientMessage): Boolean = onActor(joinCode, message, this::onMessage)

    fun unregisterClient(joinCode: String, channelId: String) = onActor(joinCode, channelId, this::removeChannel)

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

    private fun onMessage(player: Sign, message: WsClientMessage) {
        when (message) {
            is WsClientMessage.PutNew -> {
                placeLock.withLock {
                    val next = waitingFor
                    if (next == null) {
                        send(player, Error("Game already finished"))
                        return
                    }

                    if (waitingFor != player) {
                        send(player, Error("Not your turn"))
                    } else {
                        if (gameField[message.position] == null) {
                            val winRow = gameField.set(message.position, player)
                            send(NewPoint(player, message.position))
                            waitingFor = if (winRow != null) {
                                send(GameResult(player, winRow))
                                null
                            } else {
                                !next
                            }
                        } else {
                            send(player, OpponentEvent("Position '${message.position}' already occupied, try again"))
                        }
                    }
                }
            }
            is WsClientMessage.PartScanRequest -> {
                send(player, FullScanResponse(xs = gameField.positionsOf(Sign.X), os = gameField.positionsOf(Sign.O)))
            }
            is WsClientMessage.FullScanRequest -> {
                send(player, FullScanResponse(xs = gameField.positionsOf(Sign.X), os = gameField.positionsOf(Sign.O)))
            }
        }
    }

    private fun addChannel(player: Sign, channelId: String, channel: (WsServerMessage) -> Unit) {
        data(player).outChannels[channelId] = channel

        send(OpponentEvent("New client joined for $player"))
        channel(Info(player, waitingFor))
    }

    private fun removeChannel(player: Sign, channelId: String) {
        data(player).outChannels.remove(channelId)
        send(!player, OpponentEvent("Client closed for $player"))
    }

}