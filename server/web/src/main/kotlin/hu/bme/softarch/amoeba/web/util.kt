package hu.bme.softarch.amoeba.web

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response
import kotlin.reflect.full.companionObject

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).simpleName) }
}

fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}

fun <T> entity(producer: () -> T?): Response =
    producer()?.let { Response.ok(it).build() } ?: notFound()

fun <T> safeEntity(producer: () -> T?): Response = entity {
    try {
        producer()
    } catch (_: Exception) {
        null
    }
}

fun notFound(): Response = Response.status(404).build()

fun badRequest(message: String = ""): Response = Response.status(400).entity(message).build()