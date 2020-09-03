package no.nav.sifinnsynapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication(exclude = [
    ErrorMvcAutoConfiguration::class
])
@EnableKafka
@EnableTransactionManagement
@ConfigurationPropertiesScan("no.nav.sifinnsynapi")
class K9DittnavVarselApiApplication

fun main(args: Array<String>) {
    runApplication<K9DittnavVarselApiApplication>(*args)
}
