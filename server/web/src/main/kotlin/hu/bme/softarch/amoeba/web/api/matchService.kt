package hu.bme.softarch.amoeba.web.api

import hu.bme.softarch.amoeba.dto.CreatedGameData
import hu.bme.softarch.amoeba.dto.GameJoinData
import hu.bme.softarch.amoeba.dto.NewGameData
import hu.bme.softarch.amoeba.dto.Statistics
import hu.bme.softarch.amoeba.web.db.DatabaseException
import hu.bme.softarch.amoeba.web.db.DbGameRepo
import hu.bme.softarch.amoeba.web.db.GameRepo
import hu.bme.softarch.amoeba.web.util.entity
import hu.bme.softarch.amoeba.web.util.logger
import hu.bme.softarch.amoeba.web.util.safeEntity
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/matches")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
class GameHandler @JvmOverloads constructor(
        private val gameRepo: GameRepo = DbGameRepo()
) {

    private val log by logger()

    private val lobbyService: LobbyService = DbLobbyService(gameRepo = gameRepo)

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun create(params: NewGameData) = entity {
        createInternal(params)
    }

    internal fun createInternal(params: NewGameData): CreatedGameData {
        val game = lobbyService.createGame(params.tilesToWin)
        return CreatedGameData(game.id, game.invite!!, game.hostCode)
    }

    @GET
    fun join(@QueryParam("invite") invite: String) = entity {
        joinInternal(invite)
    }

    internal fun joinInternal(invite: String): GameJoinData? = lobbyService.popInvite(invite)?.let {
        GameJoinData(
            id = it.id,
            clientJoinCode = it.clientCode
        )
    }

    @GET
    @Path("stats")
    fun statistics(): Response = try {
        val (xWins, oWins) = gameRepo.getWinsBySign()
        val rounds = gameRepo.avgRoundsByToWin()

        entity {
            Statistics(
                    xWins = xWins,
                    oWins = oWins,
                    averageRoundsByTilesToWin = rounds
            )
        }
    } catch (dbe: DatabaseException) {
        log.error("Failed to fetch statistics", dbe)
        Response.serverError().build()
    }

}