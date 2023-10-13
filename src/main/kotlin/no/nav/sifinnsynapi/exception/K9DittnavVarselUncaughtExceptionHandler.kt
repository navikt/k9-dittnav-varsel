package no.nav.sifinnsynapi.exception

import org.slf4j.LoggerFactory

class K9DittnavVarselUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
    private companion object {
        private val logger = LoggerFactory.getLogger(K9DittnavVarselUncaughtExceptionHandler::class.java)
    }
    override fun uncaughtException(t: Thread, e: Throwable) {
        // Handle the uncaught exception here
        logger.error("Uncaught exception in thread ${t.name}", e)
    }
}
