package hu.bme.softarch.amoeba.web

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import hu.bme.softarch.amoeba.game.web.generated.ProjectInfo
import hu.bme.softarch.amoeba.web.Server.startServer
import kotlin.system.exitProcess

class Arguments(parser: ArgParser) {

    val version by parser.flagging("-v", "--version", help = "Prints the version")

    val webPort by parser.storing("-p", "--port", help = "Port for hosting client, default: 8080", transform = String::toInt).default(8080)

    val port by parser.storing("-w", "--ws-port", help = "Port for web socket connections, default: 8080", transform = String::toInt).default(8080)

    val clientDir by parser.storing("-c", "--client", help = "Path to host client from").default("")

}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::Arguments).run {
        if (version) {
            println("Online Amoeba Version ${ProjectInfo.version}")
            exitProcess(0)
        }

        startServer(port, webPort, clientDir)

    }
}