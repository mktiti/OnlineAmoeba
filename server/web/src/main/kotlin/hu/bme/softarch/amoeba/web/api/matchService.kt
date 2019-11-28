package hu.bme.softarch.amoeba.web.api

import hu.bme.softarch.amoeba.dto.CreatedGameData
import hu.bme.softarch.amoeba.dto.GameJoinData
import hu.bme.softarch.amoeba.dto.NewGameData
import hu.bme.softarch.amoeba.web.util.entity
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/matches")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
class GameHandler {

    private val lobbyService: LobbyService = InMemLobbyService
    private val inviteStore: InviteStore = SyncInviteStore()

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun create(params: NewGameData) = entity {
        createInternal(params)
    }

    internal fun createInternal(params: NewGameData): CreatedGameData {
        val game = lobbyService.createGame(params.tilesToWin)
        val invite = inviteStore.addGame(game)

        return CreatedGameData(id = game.id, inviteCode = invite, hostJoinCode = game.xPass)
    }

    @GET
    @Path("/{invite}")
    fun join(@PathParam("invite") invite: String) = entity {
        joinInternal(invite)
    }

    internal fun joinInternal(invite: String): GameJoinData? = inviteStore.fetch(invite)?.let {
        GameJoinData(
                id = it.id,
                clientJoinCode = it.oPass
        )
    }

}