package no.nav.sifinnsynapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource

@ConstructorBinding
@ConfigurationProperties(prefix = "kafka.onprem")
data class KafkaOnpremProperties(
    val consumer: ConsumerProperties,
    val producer: ProducerProperties,
    val servers: String,
    val properties: SecurityProperties? = null
)

data class SecurityProperties(
    val security: Security,
    val sasl: Sasl,
    val ssl: Ssl
)

data class Security (
    val protocol: String
)

data class Sasl(
    val mechanism: String,
    val jaasConfig: String,
)

data class Ssl(
    val trustStoreLocation: Resource,
    val trustStorePassword: String,
    val trustStoreType: String,
)

data class ConsumerProperties(
    val enableAutoCommit: Boolean,
    val groupId: String,
    val autoOffsetReset: String,
    val isolationLevel: String,
    val retryInterval: Long,
    val keyDeserializer: String,
    val valueDeserializer: String
)

data class ProducerProperties(
    val clientId: String,
    val keySerializer: String,
    val valueSerializer: String,
    val retries: Int,
    val schemaRegistryUrl: String
)