package hu.bme.softarch.amoeba.web.api

import hu.bme.softarch.amoeba.web.db.DbGameRepo
import hu.bme.softarch.amoeba.web.db.GameRepo
import hu.bme.softarch.amoeba.web.util.CodeGenerator
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

interface LobbyService {

    fun createGame(tilesToWin: Int): GameInfo

    fun getGame(id: Long): FullGame?

}

object DbLobbyService : LobbyService {

    private val gameRepo: GameRepo by lazy { DbGameRepo() }

    private val idCounter = AtomicLong(gameRepo.maxId() + 1)

    override fun createGame(tilesToWin: Int): GameInfo {
        return FullGame(
                info = GameInfo(
                        id = idCounter.getAndIncrement(),
                        toWin = tilesToWin,
                        lastStored = LocalDateTime.now(),
                        hostCode = CodeGenerator.generate(10),
                        clientCode = CodeGenerator.generate(10)
                ),
                data = GameData()
        ).apply {
            gameRepo.archive(this)
        }.info
    }

    override fun getGame(id: Long): FullGame? = gameRepo.fetchArchived(id)

}

object InMemLobbyService : LobbyService {

    private val idCounter = AtomicLong(0)

    private val games = mutableMapOf<Long, FullGame>()

    override fun createGame(tilesToWin: Int): GameInfo = GameInfo(
            id = idCounter.getAndIncrement(),
            toWin = tilesToWin,
            lastStored = LocalDateTime.now(),
            hostCode = CodeGenerator.generate(10),
            clientCode = CodeGenerator.generate(10)
    ).apply {
        games[id] = FullGame(this, GameData())
    }

    override fun getGame(id: Long) = games[id]

}