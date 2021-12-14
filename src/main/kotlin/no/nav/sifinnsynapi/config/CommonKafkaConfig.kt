package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.konsumenter.K9Beskjed
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.Logger
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.kafka.support.converter.JsonMessageConverter
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration
import java.util.function.BiConsumer

class CommonKafkaConfig {
    companion object {

        fun commonConfig(kafkaConfigProps: KafkaConfigProperties) = mutableMapOf<String, Any>().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigProps.servers)
        } + securityConfig(kafkaConfigProps.properties)

        fun securityConfig(securityProps: KafkaProperties?) = mutableMapOf<String, Any>().apply {
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "") // Disable server host name verification
            securityProps?.let { props: KafkaProperties ->
                val sslProps = props.ssl

                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, props.security.protocol)
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslProps.truststoreLocation.file.absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslProps.truststorePassword)
                put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, sslProps.truststoreType)

                props.sasl?.let { saslProps: KafkaSaslProperties ->
                    put(SaslConfigs.SASL_MECHANISM, saslProps.mechanism)
                    put(SaslConfigs.SASL_JAAS_CONFIG, saslProps.jaasConfig)
                }

                sslProps.keystoreLocation?.let { put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, it.file.absolutePath) }
                sslProps.keystorePassword?.let { put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it) }
                sslProps.keystoreType?.let { put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, it) }
            }
        }

        fun consumerFactory(kafkaConfigProps: KafkaConfigProperties): ConsumerFactory<String, String> {
            val consumerProps = kafkaConfigProps.consumer
            return DefaultKafkaConsumerFactory(
                mutableMapOf<String, Any>(
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to consumerProps.enableAutoCommit,
                    ConsumerConfig.GROUP_ID_CONFIG to consumerProps.groupId,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to consumerProps.autoOffsetReset,
                    ConsumerConfig.ISOLATION_LEVEL_CONFIG to consumerProps.isolationLevel,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to consumerProps.keyDeserializer,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to consumerProps.valueDeserializer
                ) + commonConfig(kafkaConfigProps)
            )
        }

        fun producerFactory(kafkaConfigProps: KafkaConfigProperties): ProducerFactory<Nokkel, Beskjed> {
            val producerProps = kafkaConfigProps.producer
            return DefaultKafkaProducerFactory(
                mutableMapOf<String, Any>(
                    ProducerConfig.CLIENT_ID_CONFIG to producerProps.clientId,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to producerProps.keySerializer,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to producerProps.valueSerializer,
                    ProducerConfig.RETRIES_CONFIG to producerProps.retries,
                    "schema.registry.url" to producerProps.schemaRegistryUrl
                ) + commonConfig(kafkaConfigProps)
            )
        }

        fun kafkaTemplate(producerFactory: ProducerFactory<Nokkel, Beskjed>) = KafkaTemplate(producerFactory)

        fun configureConcurrentKafkaListenerContainerFactory(
            consumerFactory: ConsumerFactory<String, String>,
            retryInterval: Long,
            kafkaTemplate: KafkaTemplate<Nokkel, Beskjed>,
            objectMapper: ObjectMapper,
            logger: Logger
        ): ConcurrentKafkaListenerContainerFactory<String, String> {
            val factory = ConcurrentKafkaListenerContainerFactory<String, String>()

            factory.consumerFactory = consumerFactory

            factory.setReplyTemplate(kafkaTemplate)

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#payload-conversion-with-batch
            factory.setMessageConverter(JsonMessageConverter(objectMapper))

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#exactly-once
            factory.containerProperties.eosMode = ContainerProperties.EOSMode.BETA

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#committing-offsets
            factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD;

            // https://docs.spring.io/spring-kafka/reference/html/#listener-container
            factory.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(10L))

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#delivery-header
            factory.containerProperties.isDeliveryAttemptHeader = true

            // https://docs.spring.io/spring-kafka/reference/html/#seek-to-current
            factory.setErrorHandler(SeekToCurrentErrorHandler(recoverer(logger), FixedBackOff(retryInterval, Long.MAX_VALUE)))

            factory.setRecordFilterStrategy {
                val melding = objectMapper.readValue(it.value(), K9Beskjed::class.java)
                val correlationId = melding.metadata.correlationId
                MDCUtil.toMDC(Constants.CORRELATION_ID, correlationId)
                MDCUtil.toMDC(Constants.NAV_CONSUMER_ID, "k9-dittnav-varsel")

                false
            }

            return factory
        }

        private fun recoverer(logger: Logger) = BiConsumer { cr: ConsumerRecord<*, *>, ex: Exception ->
            logger.error("Retry attempts exhausted for ${cr.topic()}-${cr.partition()}@${cr.offset()}", ex)
        }
    }
}
