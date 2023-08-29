package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.avroProducerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.configureConcurrentKafkaListenerContainerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.consumerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.stringProducerFactory
import no.nav.sifinnsynapi.konsumenter.K9Beskjed
import no.nav.sifinnsynapi.konsumenter.K9Utkast
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class AivenKafkaConfig(
    private val objectMapper: ObjectMapper,
    private val kafkaClusterProperties: KafkaClusterProperties,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AivenKafkaConfig::class.java)
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> = consumerFactory(kafkaClusterProperties.aiven)

    @Bean
    fun producerFactory(): ProducerFactory<String, String> = stringProducerFactory(kafkaClusterProperties.aiven)

    @Bean
    fun beskjedProducerFactory(): ProducerFactory<NokkelInput, BeskjedInput> =
        avroProducerFactory(kafkaClusterProperties.aiven)

    @Bean
    fun beskjedKafkaTemplate(beskjedProducerFactory: ProducerFactory<NokkelInput, BeskjedInput>): KafkaTemplate<NokkelInput, BeskjedInput> =
        CommonKafkaConfig.kafkaTemplate(beskjedProducerFactory)

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> =
        CommonKafkaConfig.kafkaTemplate(producerFactory)

    @Bean
    fun beskjedKafkaJsonListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
        beskjedKafkaTemplate: KafkaTemplate<NokkelInput, BeskjedInput>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        configureConcurrentKafkaListenerContainerFactory<K9Beskjed>(
            consumerFactory = consumerFactory,
            kafkaTemplate = beskjedKafkaTemplate,
            retryInterval = kafkaClusterProperties.aiven.consumer.retryInterval,
            objectMapper = objectMapper,
            logger = logger,
            correlationIdExtractor = { it.metadata.correlationId }
        )

    @Bean
    fun utkastKafkaJsonListenerContainerFactory(
        aivenConsumerFactory: ConsumerFactory<String, String>,
        kafkaTemplate: KafkaTemplate<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        configureConcurrentKafkaListenerContainerFactory<K9Utkast>(
            consumerFactory = aivenConsumerFactory,
            kafkaTemplate = kafkaTemplate,
            retryInterval = kafkaClusterProperties.aiven.consumer.retryInterval,
            objectMapper = objectMapper,
            logger = logger,
            correlationIdExtractor = { it.metadata.correlationId }
        )
}
