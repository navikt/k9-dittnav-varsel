package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.konsumenter.K9Beskjed
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.Logger
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ConsumerRecordRecoverer
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor
import org.springframework.kafka.support.converter.JsonMessageConverter
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration

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

                put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslProps.keystoreLocation.file.absolutePath)
                put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslProps.keystorePassword)
                put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, sslProps.keystoreType)
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

        fun producerFactory(kafkaConfigProps: KafkaConfigProperties): ProducerFactory<NokkelInput, BeskjedInput> {
            val producerProps = kafkaConfigProps.producer
            return DefaultKafkaProducerFactory(
                mutableMapOf<String, Any>(
                    ProducerConfig.CLIENT_ID_CONFIG to producerProps.clientId,
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to producerProps.keySerializer,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to producerProps.valueSerializer,
                    ProducerConfig.RETRIES_CONFIG to producerProps.retries,
                    KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to producerProps.schemaRegistryUrl,
                    KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
                    KafkaAvroDeserializerConfig.USER_INFO_CONFIG to "${producerProps.schemaRegistryUser}:${producerProps.schemaRegistryPassword}"
                ) + commonConfig(kafkaConfigProps)
            )
        }

        fun kafkaTemplate(producerFactory: ProducerFactory<NokkelInput, BeskjedInput>) = KafkaTemplate(producerFactory)

        fun configureConcurrentKafkaListenerContainerFactory(
            consumerFactory: ConsumerFactory<String, String>,
            retryInterval: Long,
            kafkaTemplate: KafkaTemplate<NokkelInput, BeskjedInput>,
            objectMapper: ObjectMapper,
            logger: Logger
        ): ConcurrentKafkaListenerContainerFactory<String, String> {
            val factory = ConcurrentKafkaListenerContainerFactory<String, String>()

            factory.consumerFactory = consumerFactory

            factory.setReplyTemplate(kafkaTemplate)

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#payload-conversion-with-batch
            factory.setMessageConverter(JsonMessageConverter(objectMapper))

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#committing-offsets
            factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD

            // https://docs.spring.io/spring-kafka/reference/html/#listener-container
            factory.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(10L))

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#delivery-header
            factory.containerProperties.isDeliveryAttemptHeader = true

            // https://docs.spring.io/spring-kafka/reference/html/#seek-to-current
            factory.setAfterRollbackProcessor(defaultAfterRollbackProsessor(logger, retryInterval))

            factory.setRecordInterceptor { record, _ ->
                val melding = objectMapper.readValue(record.value(), K9Beskjed::class.java)
                val correlationId = melding.metadata.correlationId
                MDCUtil.toMDC(Constants.CORRELATION_ID, correlationId)
                MDCUtil.toMDC(Constants.NAV_CONSUMER_ID, "k9-dittnav-varsel")
                record
            }

            return factory
        }

        private fun defaultAfterRollbackProsessor(logger: Logger, retryInterval: Long) =
            DefaultAfterRollbackProcessor<String, String>(
                recoverer(logger), FixedBackOff(retryInterval, Long.MAX_VALUE)
            ).apply {
                setClassifications(mapOf(), true)
            }

        private fun recoverer(logger: Logger) = ConsumerRecordRecoverer { cr: ConsumerRecord<*, *>, ex: Exception ->
            logger.error("Retry attempts exhausted for ${cr.topic()}-${cr.partition()}@${cr.offset()}", ex)
        }
    }
}
