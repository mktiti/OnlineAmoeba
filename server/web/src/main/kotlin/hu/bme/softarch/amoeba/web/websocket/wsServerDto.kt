package hu.bme.softarch.amoeba.web.websocket

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import hu.bme.softarch.amoeba.game.Pos
import hu.bme.softarch.amoeba.game.Sign

data class ScanBound(
        val min: Long,
        val max: Long
)

data class ScanBounds(
        val x: ScanBound,
        val y: ScanBound
)

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
            val bounds: ScanBounds
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

    @JsonTypeName("opponent-event")
    data class OpponentEvent(
            val event: String
    ) : WsServerMessage()

    @JsonTypeName("error")
    data class Error(
            val message: String
    ) : WsServerMessage()

}