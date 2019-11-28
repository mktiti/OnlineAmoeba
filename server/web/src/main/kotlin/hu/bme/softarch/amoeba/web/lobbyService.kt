package hu.bme.softarch.amoeba.web

interface LobbyService {

    fun createGame(tilesToWin: Int): GameInfo

}