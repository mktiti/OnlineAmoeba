package hu.bme.softarch.amoeba.web.api

import hu.bme.softarch.amoeba.web.db.DatabaseException
import hu.bme.softarch.amoeba.web.db.DbGameRepo
import hu.bme.softarch.amoeba.web.util.CodeGenerator
import hu.bme.softarch.amoeba.web.util.loop
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface LobbyService {

    fun createGame(tilesToWin: Int): GameInfo

    fun getGame(id: Long): FullGame?

    fun popInvite(invite: String): GameInfo?

}

class DbLobbyService(
        private val inviteLength: Int = 6,
        private val joinCodeLength: Int = 10,
        private val gameRepo: DbGameRepo = DbGameRepo()
) : LobbyService {

    companion object {
        private val placeholderGame = GameInfo(-1, LocalDateTime.now(), 0, "", "", "")
    }

    private val inviteStore: MutableMap<String, GameInfo> = mutableMapOf()
    private val inviteLock = ReentrantLock()

    override fun createGame(tilesToWin: Int): GameInfo {
        val invite: String = inviteLock.withLock {
            loop {
                val invite = CodeGenerator.generate(inviteLength, CodeGenerator.digitAlphabet)
                if (inviteStore[invite] == null) {
                    inviteStore[invite] = placeholderGame
                    return@withLock invite
                }
            }
        }

        try {
            return gameRepo.create(
                    GameInfo(
                            id = -1L,
                            toWin = tilesToWin,
                            lastStored = LocalDateTime.now(),
                            hostCode = CodeGenerator.generate(joinCodeLength),
                            clientCode = CodeGenerator.generate(joinCodeLength),
                            invite = invite
                    )
            ).apply {
                inviteStore[invite] = this
            }
        } catch (dbe: DatabaseException) {
            inviteLock.withLock {
                inviteStore.remove(invite)
            }
            throw dbe
        }
    }

    override fun getGame(id: Long): FullGame? = gameRepo.fetchArchived(id)

    override fun popInvite(invite: String): GameInfo? {
        val game: GameInfo = inviteLock.withLock {
            inviteStore[invite]
        } ?: return null

        gameRepo.removeInvite(game.id)

        inviteLock.withLock {
            inviteStore.remove(game.invite)
        }

        return game
    }
}
