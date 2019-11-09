package hu.bme.softarch.amoeba.web

import java.time.LocalDateTime

data class GameInfo(
    val id: String,
    val createdAt: LocalDateTime,

    val toWin: Int,

    val xPass: String,
    val yPass: String
)