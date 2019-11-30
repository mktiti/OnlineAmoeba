package hu.bme.softarch.amoeba.web.util

import javax.ws.rs.core.Response

fun <T> entity(producer: () -> T?): Response =
    producer()?.let { Response.ok(it).build() } ?: notFound()

fun <T> safeEntity(producer: () -> T?): Response = entity {
    try {
        producer()
    } catch (_: Exception) {
        null
    }
}

inline fun loop(code: () -> Unit): Nothing {
    while (true) {
        code()
    }
}

fun notFound(): Response = Response.status(404).build()

fun badRequest(message: String = ""): Response = Response.status(400).entity(message).build()