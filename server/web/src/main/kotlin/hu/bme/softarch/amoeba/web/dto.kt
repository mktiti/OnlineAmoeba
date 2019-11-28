package hu.bme.softarch.amoeba.web

import com.fasterxml.jackson.annotation.JsonProperty

data class NewGameData(
    @JsonProperty("tilesToWin")
    val tilesToWin: Int
)

data class CreatedGameData(
    val inviteCode: String,
    val hostJoinCode: String
)

data class GameJoinData(
    val clientJoinCode: String
)