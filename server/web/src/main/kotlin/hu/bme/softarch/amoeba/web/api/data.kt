package hu.bme.softarch.amoeba.web.api

import hu.bme.softarch.amoeba.game.Pos
import java.time.LocalDateTime

data class GameInfo(
    val id: Long,
    val createdAt: LocalDateTime,

    val toWin: Int,

    val xPass: String,
    val oPass: String
)

data class GameData(
    val xTiles: List<Pos> = emptyList(),
    val oTiles: List<Pos> = emptyList()
)

data class FullGame(
        val info: GameInfo,
        val data: GameData
)