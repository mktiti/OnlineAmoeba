package hu.bme.softarch.amoeba.web.db

import hu.bme.softarch.amoeba.game.Pos
import hu.bme.softarch.amoeba.web.api.FullGame
import hu.bme.softarch.amoeba.web.api.GameData
import hu.bme.softarch.amoeba.web.api.GameInfo
import hu.bme.softarch.amoeba.web.util.logger
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.PreparedBatch
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import java.math.BigInteger
import java.nio.file.Path

interface GameRepo {

    fun fetchArchived(id: Long): FullGame?

    fun archive(game: FullGame)

    fun maxId(): Long

}

object DbContextHolder {

    private const val DB_INIT_FILE = "/sql/create-db.sql"

    private val connectStringMake: (Path) -> String = { "jdbc:hsqldb:file:${it.toAbsolutePath()}/amoeba-db/;shutdown=true" }

    private val log by logger()

    var connection: Jdbi? = null
        private set

    @Synchronized
    fun initDb(path: Path) {
        if (connection == null) {
            connection = Jdbi.create(connectStringMake(path)).apply {
                log.info("Database location: ${path.toAbsolutePath()}")

                installPlugin(KotlinPlugin())
                installPlugin(KotlinSqlObjectPlugin())
                registerColumnMapper(BigInteger::class.java) { rs, col, _ -> rs.getLong(col) }

                useTransaction<Exception> { handle ->
                    handle.createScript(DbContextHolder::class.java.getResource(DB_INIT_FILE).readText()).execute()
                }

                log.info("Database init script executed")
            }
        } else {
            throw IllegalStateException("Database connection is already initialized!")
        }
    }

}

class DbGameRepo(private val jdbi: Jdbi = DbContextHolder.connection!!) : GameRepo {

    override fun maxId(): Long = jdbi.withHandle<Long, Exception> { handle ->
        handle.createQuery("select max(id) from Game").mapTo<Long>().findOne().orElse(0)
    }

    override fun fetchArchived(id: Long): FullGame? = jdbi.withHandle<FullGame?, Exception> { handle ->
        val info: GameInfo = handle.createQuery("select id, lastStored, hostCode, clientCode, toWin from Game where id = :id")
                .bind("id", id).mapTo<GameInfo>().findOne().orElse(null) ?: return@withHandle null

        fun tiles(isX: Boolean): List<Pos> =
                handle.createQuery("select posX as x, posY as y from Tile where gameId = :gameId and isX = :isX")
                    .bind("gameId", id).bind("isX", isX)
                    .mapTo<Pos>().list()

        FullGame(info, GameData(tiles(true), tiles(false)))
    }

    override fun archive(game: FullGame) = jdbi.useTransaction<Exception> { handle ->
        handle.createUpdate("insert into Game (id, lastStored, hostCode, clientCode, toWin) values (:id, :time, :host, :client, :toWin)")
                .bind("id", game.info.id)
                .bind("time", game.info.lastStored)
                .bind("host", game.info.hostCode)
                .bind("client", game.info.clientCode)
                .bind("toWin", game.info.toWin)
                .execute()

        handle.prepareBatch("insert into Tile (gameId, posX, posY, isX) values (:gameId, :x, :y, :isX)").apply {
            fun PreparedBatch.bind(tiles: Collection<Pos>, isX: Boolean) {
                val gameId = game.info.id
                tiles.forEach { (x, y) ->
                    bind("x", x).bind("y", y).bind("gameId", gameId).bind("isX", isX).add()
                }
            }

            bind(game.data.xTiles, true)
            bind(game.data.oTiles, false)

            execute()
        }
    }
}
