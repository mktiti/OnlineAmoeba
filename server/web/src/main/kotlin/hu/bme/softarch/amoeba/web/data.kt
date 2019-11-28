package hu.bme.softarch.amoeba.web

import hu.bme.softarch.amoeba.game.Pos
import java.time.LocalDateTime

data class GameInfo(
    val id: String,
    val createdAt: LocalDateTime,

    val toWin: Int,

    val xPass: String,
    val yPass: String
)

data class GameData(
    val xTiles: List<Pos>,
    val yTiles: List<Pos>
)

data class FullGame(
    val info: GameInfo,
    val data: GameData
)