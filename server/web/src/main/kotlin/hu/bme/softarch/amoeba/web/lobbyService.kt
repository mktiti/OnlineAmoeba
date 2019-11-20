package hu.bme.softarch.amoeba.web

import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

interface LobbyService {

    fun createGame(tilesToWin: Int): GameInfo

    fun getGame(id: Long): FullGame?

}

object InMemLobbyService : LobbyService {

    private val idCounter = AtomicLong(0)

    private val games = mutableMapOf<Long, FullGame>()

    override fun createGame(tilesToWin: Int): GameInfo = GameInfo(
            id = idCounter.getAndIncrement(),
            toWin = tilesToWin,
            createdAt = LocalDateTime.now(),
            //xPass = CodeGenerator.generate(10),
            //oPass = CodeGenerator.generate(10)

            xPass = "asd",
            oPass = "123"
    ).apply {
        games[id] = FullGame(this, GameData())
    }

    override fun getGame(id: Long) = games[id]

}