package no.nav.sifinnsynapi.http

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.zalando.problem.Problem
import org.zalando.problem.Status
import org.zalando.problem.spring.web.advice.AdviceTrait
import org.zalando.problem.spring.web.advice.ProblemHandling
import java.net.URI

@ControllerAdvice
class ExceptionHandler : ProblemHandling, AdviceTrait {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)
    }

    @ExceptionHandler(value = [Exception::class])
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun håndtereGeneriskException(exception: Exception, request: ServletWebRequest): ResponseEntity<Problem> {
        log(HttpStatus.INTERNAL_SERVER_ERROR, exception, request)
        return create(Status.INTERNAL_SERVER_ERROR, exception, request, URI("/problem-details/internal-server-error"))
    }

    fun log(status: HttpStatus, exception: Exception, request: WebRequest) {
        logger.error("{} - {}, {}", status, exception, request.toString())
    }
}
