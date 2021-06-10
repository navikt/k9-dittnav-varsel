package no.nav.sifinnsynapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource

@ConstructorBinding
@ConfigurationProperties(prefix = "kafka.aiven")
data class KafkaAivenProperties(
    val servers: String,
    val properties: SecurityPropertiesAiven? = null,
    val consumer: ConsumerProperties,
    val producer: ProducerProperties,
)

data class SecurityPropertiesAiven(
    val security: Security,
    val ssl: SslAiven
)

data class SslAiven(
    val truststoreLocation: Resource,
    val truststorePassword: String,
    val truststoreType: String,
    val keystoreLocation: Resource,
    val keystorePassword: String,
    val keystoreType: String
)