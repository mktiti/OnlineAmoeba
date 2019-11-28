package hu.bme.softarch.amoeba.web

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import hu.bme.softarch.amoeba.game.web.generated.ProjectInfo
import hu.bme.softarch.amoeba.web.api.GameHandler
import hu.bme.softarch.amoeba.web.api.NewGameData
import hu.bme.softarch.amoeba.web.util.Server.startServer
import hu.bme.softarch.amoeba.web.util.setLogLocation
import kotlin.system.exitProcess

class Arguments(parser: ArgParser) {

    val version by parser.flagging("-v", "--version", help = "Prints the version")

    val port by parser.storing("-p", "--port", help = "TCP port to use, default: 8080", transform = String::toInt).default(8080)

    val clientDir by parser.storing("-c", "--client", help = "Path to host client from").default<String?>(null)

    val logLocation by parser.storing("-l", "--log", help = "Log file destination").default("log")

}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::Arguments).run {
        if (version) {
            println("Online Amoeba Version ${ProjectInfo.version}")
            exitProcess(0)
        }

        setLogLocation(logLocation)

        val testHandler = GameHandler()
        val game = testHandler.createInternal(NewGameData(tilesToWin = 5))
        val clientJoin = testHandler.joinInternal(game.inviteCode)?.clientJoinCode

        println("Test game ready: Id: ${game.id}, host join: ${game.hostJoinCode}, client join: $clientJoin")

        startServer(port, clientDir)

    }
}