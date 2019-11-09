package hu.bme.softarch.amoeba.web

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.reflect.full.companionObject

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).simpleName) }
}

fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}

fun setLogLocation(logLocation: String) {
    val logPath = Paths.get(logLocation, "amoeba-${LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)}.log")
    println("Logging to ${logPath.toAbsolutePath()}")
    System.setProperty("log.file", logPath.toAbsolutePath().toString())
}