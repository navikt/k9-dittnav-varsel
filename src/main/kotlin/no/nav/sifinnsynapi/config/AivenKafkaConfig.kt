package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.configureConcurrentKafkaListenerContainerFactory
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*


@Configuration
class AivenKafkaConfig(
    private val objectMapper: ObjectMapper,
    private val kafkaAivenProperties: KafkaAivenProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AivenKafkaConfig::class.java)
    }

    fun commonConfig() = mutableMapOf<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaAivenProperties.servers)
    } + securityConfig()

    fun securityConfig() = mutableMapOf<String, Any>().apply {
        put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "") // Disable server host name verification
        kafkaAivenProperties.properties?.let {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it.security.protocol)
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it.ssl.truststoreLocation)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it.ssl.truststorePassword)
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, it.ssl.truststoreType)
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, it.ssl.keystoreLocation)
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it.ssl.keystorePassword)
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, it.ssl.keystoreType)
        }
    }

    @Bean
    fun aivenConsumerFactory(): ConsumerFactory<String, String> {
        val consumerProperties = mutableMapOf<String, Any>(
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to kafkaAivenProperties.consumer.enableAutoCommit,
            ConsumerConfig.GROUP_ID_CONFIG to kafkaAivenProperties.consumer.groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to kafkaAivenProperties.consumer.autoOffsetReset,
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to kafkaAivenProperties.consumer.isolationLevel,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to kafkaAivenProperties.consumer.keyDeserializer,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to kafkaAivenProperties.consumer.valueDeserializer
        ) + commonConfig()

        return DefaultKafkaConsumerFactory(consumerProperties)
    }

    @Bean
    fun aivenProducerFactory(): ProducerFactory<Nokkel, Beskjed> {
        val producerProperties = mutableMapOf<String, Any>(
            ProducerConfig.CLIENT_ID_CONFIG to kafkaAivenProperties.producer.clientId,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to kafkaAivenProperties.producer.keySerializer,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to kafkaAivenProperties.producer.valueSerializer,
            ProducerConfig.RETRIES_CONFIG to kafkaAivenProperties.producer.retries,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true, // Den sikrer rekkefølge
            ProducerConfig.ACKS_CONFIG to "all", // Den sikrer at data ikke mistes
            "schema.registry.url" to kafkaAivenProperties.producer.schemaRegistryUrl
        ) + commonConfig()

        return DefaultKafkaProducerFactory<Nokkel, Beskjed>(producerProperties)
    }

    @Bean
    fun aivenKafkaTemplate(onpremProducerFactory: ProducerFactory<Nokkel, Beskjed>): KafkaTemplate<Nokkel, Beskjed> {
        return KafkaTemplate(onpremProducerFactory)
    }

    @Bean
    fun aivenKafkaJsonListenerContainerFactory(
        aivenConsumerFactory: ConsumerFactory<String, String>,
        aivenKafkaTemplate: KafkaTemplate<Nokkel, Beskjed>
    ): ConcurrentKafkaListenerContainerFactory<String, String> = configureConcurrentKafkaListenerContainerFactory(
        consumerFactory = aivenConsumerFactory,
        kafkaTemplate = aivenKafkaTemplate,
        retryInterval = kafkaAivenProperties.consumer.retryInterval,
        objectMapper = objectMapper,
        logger = logger)
}