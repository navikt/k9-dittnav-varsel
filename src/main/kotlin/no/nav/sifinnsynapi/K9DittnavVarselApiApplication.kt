package no.nav.sifinnsynapi

import no.nav.sifinnsynapi.exception.K9DittnavVarselUncaughtExceptionHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication(exclude = [
    ErrorMvcAutoConfiguration::class
])
@EnableKafka
@ConfigurationPropertiesScan("no.nav.sifinnsynapi")
class K9DittnavVarselApiApplication

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler(K9DittnavVarselUncaughtExceptionHandler())
    runApplication<K9DittnavVarselApiApplication>(*args)
}
