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
    val security: Security? = null,
    val sasl: Sasl? = null,
    val ssl: Ssl? = null
)

data class Security (
    val protocol: String? = null
)

data class Sasl(
    val mechanism: String? = null,
    val jaasConfig: String? = null,
)

data class Ssl(
    val trustStoreLocation: Resource? = null,
    val trustStorePassword: String? = null,
    val trustStoreType: String? = null,
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