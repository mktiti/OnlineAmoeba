package hu.bme.softarch.amoeba.web.db

import hu.bme.softarch.amoeba.game.Pos
import hu.bme.softarch.amoeba.game.Sign
import hu.bme.softarch.amoeba.web.api.FullGame
import hu.bme.softarch.amoeba.web.api.GameData
import hu.bme.softarch.amoeba.web.api.GameInfo
import hu.bme.softarch.amoeba.web.util.logger
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.PreparedBatch
import java.lang.Exception
import java.time.LocalDateTime

interface GameRepo {

    data class SignWins(val xWins: Int, val yWins: Int)

    fun create(game: GameInfo): GameInfo

    fun archive(game: FullGame)

    fun fetchArchived(id: Long): FullGame?

    fun removeInvite(id: Long)

    fun saveResult(id: Long, winner: Sign, rounds: Int)

    fun getWinsBySign(): SignWins

    fun avgRoundsByToWin(): Map<Int, Float>

    fun deleteInactive(activeGames: Collection<Long>, keepTime: Int)

}

class DbGameRepo(private val jdbi: Jdbi = DbContextHolder.connection!!) : GameRepo {

    override fun fetchArchived(id: Long): FullGame? = jdbi.withHandle<FullGame?, DatabaseException> { handle ->
        val info: GameInfo = handle.createQuery("select id, lastStored, hostCode, clientCode, toWin, invite from Game where id = :id")
                .bind("id", id).mapTo<GameInfo>().findOne().orElse(null) ?: return@withHandle null

        fun tiles(isX: Boolean): List<Pos> =
                handle.createQuery("select posX as x, posY as y from Tile where gameId = :gameId and isX = :isX")
                    .bind("gameId", id).bind("isX", isX)
                    .mapTo<Pos>().list()

        FullGame(info, GameData(tiles(true), tiles(false)))
    }

    override fun create(game: GameInfo): GameInfo = jdbi.withHandle<GameInfo, DatabaseException> { handle ->
        val newId = handle.createUpdate("insert into Game (lastStored, hostCode, clientCode, toWin, invite) values (:time, :host, :client, :toWin, :invite)")
                .bind("time", game.lastStored)
                .bind("host", game.hostCode)
                .bind("client", game.clientCode)
                .bind("toWin", game.toWin)
                .bind("invite", game.invite)
                .executeAndReturnGeneratedKeys("id").mapTo<Long>().one()

        game.copy(id = newId)
    }

    override fun removeInvite(id: Long) = jdbi.useHandle<DatabaseException> { handle ->
        handle.createUpdate("update Game set invite = null where id = :id").bind("id", id).execute()
    }

    override fun archive(game: FullGame) = jdbi.useTransaction<DatabaseException> { handle ->
        handle.createUpdate("update Game set lastStored = :time").bind("time", LocalDateTime.now()).execute()

        handle.createUpdate("delete from Tile where gameId = :gameId").bind("gameId", game.info.id).execute()

        handle.prepareBatch("insert into Tile (gameId, posX, posY, isX) values (:gameId, :x, :y, :isX)").apply {
            fun PreparedBatch.bind(tiles: Collection<Pos>, isX: Boolean) {
                val gameId = game.info.id
                tiles.forEach { (x, y) ->
                    bind("x", x.toLong()).bind("y", y.toLong()).bind("gameId", gameId).bind("isX", isX).add()
                }
            }

            bind(game.data.xTiles, true)
            bind(game.data.oTiles, false)

            execute()
        }
    }

    override fun saveResult(id: Long, winner: Sign, rounds: Int) = jdbi.useTransaction<DatabaseException> { handle ->
        handle.createUpdate("delete from Tile where gameId = :id").bind("id", id).execute()
        handle.createUpdate("update Game set xWin = :xWin, rounds = :rounds where id = :gameId")
                .bind("gameId", id)
                .bind("xWin", winner == Sign.X)
                .bind("rounds", rounds)
                .execute()
    }

    override fun getWinsBySign(): GameRepo.SignWins = jdbi.withHandle<GameRepo.SignWins, DatabaseException> { handle ->
        fun wins(x: Boolean): Int = handle.createQuery("select count(id) from Game where xWin = :x")
                .bind("x", x)
                .mapTo<Int>()
                .one()

        GameRepo.SignWins(wins(true), wins(false))
    }

    override fun avgRoundsByToWin(): Map<Int, Float> = jdbi.withHandle<Map<Int, Float>, DatabaseException> { handle ->
        handle.createQuery("select toWin, avg(rounds) from Game where rounds is not null group by toWin")
                .map {rs, _ ->  rs.getInt(1) to rs.getFloat(2) }
                .toMap()
    }

    override fun deleteInactive(activeGames: Collection<Long>, keepTime: Int) = jdbi.useTransaction<DatabaseException> { handle ->
        try {
            handle.createUpdate("delete from Game where xWin is null and id not in (<activeIds>) and TIMESTAMPDIFF(SQL_TSI_HOUR, lastStored, CURRENT_TIMESTAMP) > :keepTime")
                    .bindList("activeIds", if (activeGames.isEmpty()) listOf(-1) else activeGames)
                    .bind("keepTime", keepTime)
                    .execute()
        } catch (e: Exception) {
            val log by logger()
            log.error("", e)
        }
    }
}
