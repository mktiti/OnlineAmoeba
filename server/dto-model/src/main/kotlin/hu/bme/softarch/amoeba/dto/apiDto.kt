package hu.bme.softarch.amoeba.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NewGameData(
    @JsonProperty("tilesToWin")
    val tilesToWin: Int
)

data class CreatedGameData(
    val id: Long,
    val inviteCode: String,
    val hostJoinCode: String
)

data class GameJoinData(
    val id: Long,
    val clientJoinCode: String
)

data class Statistics(
    val xWins: Int,
    val oWins: Int,
    val averageRoundsByTilesToWin: Map<Int, Float>
)
