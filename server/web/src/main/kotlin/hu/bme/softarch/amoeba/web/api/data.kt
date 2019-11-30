package hu.bme.softarch.amoeba.web.api

import hu.bme.softarch.amoeba.game.Pos
import java.time.LocalDateTime

data class GameInfo(
    val id: Long,
    val lastStored: LocalDateTime,

    val toWin: Int,

    val hostCode: String,
    val clientCode: String,

    val invite: String?
)

data class GameData(
    val xTiles: List<Pos> = emptyList(),
    val oTiles: List<Pos> = emptyList()
)

data class FullGame(
    val info: GameInfo,
    val data: GameData
)