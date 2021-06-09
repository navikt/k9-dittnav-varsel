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
class OnpremKafkaConfig(
    private val objectMapper: ObjectMapper,
    private val kafkaOnpremProperties: KafkaOnpremProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(OnpremKafkaConfig::class.java)
    }

    fun commonConfig() = mutableMapOf<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaOnpremProperties.servers)
    } + securityConfig()

    fun securityConfig() = mutableMapOf<String, Any>().apply {
        kafkaOnpremProperties.properties?.securityProtocol?.let { put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it) }
        kafkaOnpremProperties.properties?.saslMechanism?.let { put(SaslConfigs.SASL_MECHANISM, it) }
        kafkaOnpremProperties.properties?.saslJaasConfig?.let { put(SaslConfigs.SASL_JAAS_CONFIG, it) }
        kafkaOnpremProperties.properties?.sslTrustStoreLocation?.let { put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it.file.absolutePath) }
        kafkaOnpremProperties.properties?.sslTrustStorePassword?.let { put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it) }
        kafkaOnpremProperties.properties?.sslTrustStoreType?.let { put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, it) }
    }

    @Bean
    fun onpremConsumerFactory(): ConsumerFactory<String, String> {
        val consumerProperties = mutableMapOf<String, Any>(
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to kafkaOnpremProperties.consumer.enableAutoCommit,
            ConsumerConfig.GROUP_ID_CONFIG to kafkaOnpremProperties.consumer.groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to kafkaOnpremProperties.consumer.autoOffsetReset,
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to kafkaOnpremProperties.consumer.isolationLevel,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to kafkaOnpremProperties.consumer.keyDeserializer,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to kafkaOnpremProperties.consumer.valueDeserializer
        ) + commonConfig()

        return DefaultKafkaConsumerFactory(consumerProperties)
    }

    @Bean
    fun onpremProducerFactory(): ProducerFactory<Nokkel, Beskjed> {
        val producerProperties = mutableMapOf<String, Any>(
            ProducerConfig.CLIENT_ID_CONFIG to kafkaOnpremProperties.producer.clientId,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to kafkaOnpremProperties.producer.keySerializer,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to kafkaOnpremProperties.producer.valueSerializer,
            ProducerConfig.RETRIES_CONFIG to kafkaOnpremProperties.producer.retries,
            "schema.registry.url" to kafkaOnpremProperties.producer.schemaRegistryUrl
        ) + commonConfig()

        val factory = DefaultKafkaProducerFactory<Nokkel, Beskjed>(producerProperties)
        return factory
    }

    @Bean
    fun onpremKafkaTemplate(onpremProducerFactory: ProducerFactory<Nokkel, Beskjed>): KafkaTemplate<Nokkel, Beskjed> {
        return KafkaTemplate(onpremProducerFactory)
    }

    @Bean
    fun onpremKafkaJsonListenerContainerFactory(
        onpremConsumerFactory: ConsumerFactory<String, String>,
        onpremKafkaTemplate: KafkaTemplate<Nokkel, Beskjed>
    ): ConcurrentKafkaListenerContainerFactory<String, String> = configureConcurrentKafkaListenerContainerFactory(
        consumerFactory = onpremConsumerFactory,
        kafkaTemplate = onpremKafkaTemplate,
        retryInterval = kafkaOnpremProperties.consumer.retryInterval,
        objectMapper = objectMapper,
        logger = logger)
}