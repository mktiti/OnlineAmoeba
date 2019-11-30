package hu.bme.softarch.amoeba.web.db

import hu.bme.softarch.amoeba.game.Pos
import hu.bme.softarch.amoeba.web.api.FullGame
import hu.bme.softarch.amoeba.web.api.GameData
import hu.bme.softarch.amoeba.web.api.GameInfo
import hu.bme.softarch.amoeba.web.util.setLogLocation
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

import java.lang.Exception

private const val INMEM_CONNECT_STRING = "jdbc:hsqldb:mem:amoeba-db-test"

class DbGameRepoTest {

    companion object {

        @BeforeAll
        @JvmStatic
        fun setUpLog() {
            setLogLocation(System.getProperty("java.io.tmpdir") + "/amoeba-unit-test/")
        }

        fun assertSameGame(a: GameInfo, b: GameInfo, ignoreId: Boolean = false, ignoreDate: Boolean = false) {
            if (!ignoreId) {
                assertEquals(a.id, b.id)
            }
            assertEquals(a.toWin, b.toWin)
            if (!ignoreDate) {
                assertEquals(a.lastStored, b.lastStored)
            }
            assertEquals(a.hostCode, b.hostCode)
            assertEquals(a.clientCode, b.clientCode)
            assertEquals(a.invite, b.invite)
        }

        fun testGames(size: Int) = (0 until size).map {
            GameInfo(-1L, LocalDateTime.now(), it + 5, "host-$it", "client-$it", "invite-$it")
        }

        fun sampleGameData(xSize: Int, oSize: Int): GameData {
            val allTiles = (0 until (xSize + oSize)).map { Pos(it, -it) }
            return GameData(allTiles.take(xSize), allTiles.takeLast(oSize))
        }

    }

    private lateinit var jdbi: Jdbi
    private lateinit var repo: GameRepo

    @BeforeEach
    fun init() {
        jdbi = DbContextHolder.createJdbi(INMEM_CONNECT_STRING, dropPrevious = true)
        repo = DbGameRepo(jdbi)
    }

    @Test
    fun `test game generated ids success`() {
        val gameCount = 10
        val startGames = testGames(gameCount)

        val savedGamed = startGames.map(repo::create)

        val paired = startGames.zip(savedGamed)
        assertEquals(gameCount, paired.size)

        paired.forEach { (init, saved) ->
            assertSameGame(init, saved, ignoreId = true)
        }

        val ids = savedGamed.map { it.id }.toSet()
        assertEquals(gameCount, ids.size)
    }

    @Test
    fun `test save reload`() {
        val gameCount = 10
        val testGames = testGames(gameCount).map(repo::create)

        testGames.forEach {
            val reloaded = repo.fetchArchived(it.id)!!.info
            assertSameGame(it, reloaded, ignoreDate = true)
        }

    }

    @Test
    fun `test invite erasure`() {
        val gameCount = 10
        val testGames = testGames(gameCount).map(repo::create)

        testGames.forEach {
            repo.removeInvite(it.id)
        }

        testGames.forEach {
            val reloaded = repo.fetchArchived(it.id)!!
            assertNull(reloaded.info.invite)
        }
    }

    @Test
    fun `test fetch bad id error`() {
        val gameCount = 10
        val testGames = testGames(gameCount).map(repo::create)
        val badId = testGames.map { it.id }.max()!! + 1

        assertNull(repo.fetchArchived(badId))
    }

    @Test
    fun `test save and reload game data`() {
        val gameCount = 10
        val testGames = testGames(gameCount).map(repo::create).mapIndexed { i, info ->
            FullGame(
                    info = info,
                    data = sampleGameData(10 + i, 20 + i)
            )
        }

        testGames.forEach {
            repo.archive(it)
        }

        testGames.forEach {
            val stored = repo.fetchArchived(it.info.id)!!.data

            assertEquals(it.data.xTiles.toSet(), stored.xTiles.toSet())
            assertEquals(it.data.oTiles.toSet(), stored.oTiles.toSet())
        }
    }

    @Test
    fun `test game removal auto cleanup`() {
        val initGameCount = 10
        val testGames = testGames(initGameCount).map(repo::create).mapIndexed { i, info ->
            FullGame(
                    info = info,
                    data = sampleGameData(10 + i, 20 + i)
            )
        }

        testGames.forEachIndexed { i, game ->
            if (i % 2 == 0) {
                repo.archive(game)
            } else if (i % 3 == 0) {
                repo.removeInvite(game.info.id)
            }
        }

        testGames.forEach {
            repo.removeGame(it.info.id)
        }

        jdbi.useTransaction<Exception> { handle ->

            val gameCount = handle.createQuery("select count(*) from Game").mapTo<Int>().one()
            val tileCount = handle.createQuery("select count(*) from Tile").mapTo<Int>().one()

            assertEquals(0, gameCount)
            assertEquals(0, tileCount)
        }
    }

}