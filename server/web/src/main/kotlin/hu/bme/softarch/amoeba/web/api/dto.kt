package hu.bme.softarch.amoeba.web.api

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
    val clientJoinCode: String
)