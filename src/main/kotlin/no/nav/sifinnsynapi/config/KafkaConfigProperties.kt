package no.nav.sifinnsynapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource

@ConstructorBinding
@ConfigurationProperties(prefix = "kafka")
data class KafkaClusterProperties (
    val aiven: KafkaConfigProperties
)

data class KafkaConfigProperties(
    val servers: String,
    val consumer: KafkaConsumerProperties,
    val producer: KafkaProducerProperties,
    val properties: KafkaProperties? = null
)

data class KafkaConsumerProperties(
    val enableAutoCommit: Boolean,
    val groupId: String,
    val autoOffsetReset: String,
    val isolationLevel: String,
    val retryInterval: Long,
    val keyDeserializer: String,
    val valueDeserializer: String
)

data class KafkaProducerProperties(
    val clientId: String,
    val keySerializer: String,
    val valueSerializer: String,
    val retries: Int,
    val schemaRegistryUrl: String,
    val schemaRegistryUser: String,
    val schemaRegistryPassword: String,
)

data class KafkaProperties(
    val security: KafkaSecurityProperties,
    val ssl: KafkaSslProperties
)

data class KafkaSecurityProperties(
    val protocol: String
)

data class KafkaSslProperties(
    val truststoreLocation: Resource,
    val truststorePassword: String,
    val truststoreType: String,
    val keystoreLocation: Resource,
    val keystorePassword: String,
    val keystoreType: String
)