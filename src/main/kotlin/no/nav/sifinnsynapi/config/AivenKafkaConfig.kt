package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.configureConcurrentKafkaListenerContainerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.consumerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.kafkaTemplate
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.producerFactory
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
    private val kafkaClusterProperties: KafkaClusterProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AivenKafkaConfig::class.java)
    }

    @Bean
    fun aivenConsumerFactory(): ConsumerFactory<String, String> = consumerFactory(kafkaClusterProperties.aiven)

    @Bean
    fun aivenProducerFactory(): ProducerFactory<NokkelInput, BeskjedInput> = producerFactory(kafkaClusterProperties.aiven)

    @Bean
    fun aivenKafkaTemplate(aivenProducerFactory: ProducerFactory<NokkelInput, BeskjedInput>): KafkaTemplate<NokkelInput, BeskjedInput> =
        kafkaTemplate(aivenProducerFactory)

    @Bean
    fun aivenKafkaJsonListenerContainerFactory(
        aivenConsumerFactory: ConsumerFactory<String, String>,
        aivenKafkaTemplate: KafkaTemplate<NokkelInput, BeskjedInput>
    ): ConcurrentKafkaListenerContainerFactory<String, String> = configureConcurrentKafkaListenerContainerFactory(
        consumerFactory = aivenConsumerFactory,
        kafkaTemplate = aivenKafkaTemplate,
        retryInterval = kafkaClusterProperties.aiven.consumer.retryInterval,
        objectMapper = objectMapper,
        logger = logger
    )
}
