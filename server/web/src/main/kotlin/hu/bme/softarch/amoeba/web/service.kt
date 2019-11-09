package hu.bme.softarch.amoeba.web

import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/matches")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
class GameHandler {

    private val lobbyService: LobbyService = TODO()
    private val inviteStore: InviteStore = SyncInviteStore()

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun create(params: NewGameData) = entity {
        val game = lobbyService.createGame(params.tilesToWin)
        val invite = inviteStore.addGame(game)

        CreatedGameData(inviteCode = invite, hostJoinCode = game.xPass)
    }

    @GET
    @Path("/:invite")
    fun join(@QueryParam("invite") invite: String) = entity {
        inviteStore.fetch(invite)?.yPass
    }

}