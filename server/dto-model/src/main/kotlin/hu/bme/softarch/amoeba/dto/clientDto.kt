package hu.bme.softarch.amoeba.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import hu.bme.softarch.amoeba.game.FieldRange
import hu.bme.softarch.amoeba.game.Pos

@Suppress("unused")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(WsClientMessage.PutNew::class),
        JsonSubTypes.Type(WsClientMessage.FullScanRequest::class),
        JsonSubTypes.Type(WsClientMessage.PartScanRequest::class)
)
sealed class WsClientMessage {

    @JsonTypeName("put")
    data class PutNew(
            val position: Pos
    ) : WsClientMessage()

    @JsonTypeName("full-scan")
    object FullScanRequest : WsClientMessage()

    @JsonTypeName("part-scan")
    data class PartScanRequest(
            val range: FieldRange
    ) : WsClientMessage()

}