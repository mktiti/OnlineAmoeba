package hu.bme.softarch.amoeba.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import hu.bme.softarch.amoeba.game.FieldRange
import hu.bme.softarch.amoeba.game.Pos
import hu.bme.softarch.amoeba.game.Sign

@Suppress("unused")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class WsServerMessage {

    @JsonTypeName("info")
    data class Info(
            val sign: Sign,
            val waitingFor: Sign?
    ) : WsServerMessage()

    abstract class ScanResponse(
            val xs: Collection<Pos>,
            val os: Collection<Pos>
    ) : WsServerMessage()

    @JsonTypeName("part-scan")
    class PartScanResponse(
            xs: Collection<Pos>,
            os: Collection<Pos>,
            val bounds: FieldRange
    ) : ScanResponse(xs, os)

    @JsonTypeName("full-scan")
    class FullScanResponse(
            xs: Collection<Pos>,
            os: Collection<Pos>
    ) : ScanResponse(xs, os)

    @JsonTypeName("new-point")
    data class NewPoint(
            val sign: Sign,
            val position: Pos
    ) : WsServerMessage()

    @JsonTypeName("event")
    data class OpponentEvent(
            val event: String
    ) : WsServerMessage()

    @JsonTypeName("game-result")
    data class GameResult(
            val sign: Sign,
            val row: List<Pos>
    ) : WsServerMessage()

    @JsonTypeName("error")
    data class Error(
            val message: String
    ) : WsServerMessage()

    @JsonTypeName("pong")
    object Pong : WsServerMessage()

}