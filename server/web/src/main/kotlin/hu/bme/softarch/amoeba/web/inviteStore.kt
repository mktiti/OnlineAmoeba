package hu.bme.softarch.amoeba.web

import java.util.concurrent.locks.ReentrantLock

interface InviteStore {

    fun addGame(game: GameInfo): String

    fun fetch(invite: String): GameInfo?

}

class SyncInviteStore(
    private val inviteLength: Int = 6
) : InviteStore {

    private val data: MutableMap<String, GameInfo> = mutableMapOf()
    private val lock = ReentrantLock()

    override fun addGame(game: GameInfo): String {
        synchronized(lock) {
            while (true) {
                val invite = CodeGenerator.generate(inviteLength, CodeGenerator.digitAlphabet)
                if (data[invite] == null) {
                    data[invite] = game
                    return invite
                }
            }
        }
    }

    override fun fetch(invite: String): GameInfo? {
        synchronized(lock) {
            return data.remove(invite)
        }
    }

}