package hu.bme.softarch.amoeba.web.db

import hu.bme.softarch.amoeba.web.util.logger
import hu.bme.softarch.amoeba.web.websocket.MatchClientConnector
import java.lang.IllegalStateException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object CleanService {

    private val log by logger()

    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var job: ScheduledFuture<*>? = null
    private val gameRepo: GameRepo = DbGameRepo()

    private var keepTimeHours: Int = 0

    @Synchronized
    fun start(interval: Int, unit: TimeUnit, keepTimeHours: Int) {
        if (job == null) {
            this.keepTimeHours = keepTimeHours
            job = executor.scheduleAtFixedRate(this::clean, 0, interval.toLong(), unit)
            log.info("Cleanup scheduled for every $interval $unit, purging games inactive for $keepTimeHours hours")
        } else {
            throw IllegalStateException("Clean service already started")
        }
    }

    private fun clean() {
        log.info("Performing game cleanup")
        MatchClientConnector.withGameLock { activeIds ->
            try {
                gameRepo.deleteInactive(activeIds, keepTimeHours)
            } catch (dbe: DatabaseException) {
                log.error("Failed to perform cleanup", dbe)
            }
        }
        log.info("Game cleanup done")
    }

}