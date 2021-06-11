package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.configureConcurrentKafkaListenerContainerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.consumerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.kafkaTemplate
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.kafkaTransactionManager
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.producerFactory
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.transaction.KafkaTransactionManager

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
    fun aivenProducerFactory(): ProducerFactory<Nokkel, Beskjed> = producerFactory(kafkaClusterProperties.aiven)

    @Bean
    fun aivenKafkaTemplate(aivenProducerFactory: ProducerFactory<Nokkel, Beskjed>): KafkaTemplate<Nokkel, Beskjed> =
        kafkaTemplate(aivenProducerFactory,kafkaClusterProperties.aiven)

    @Bean
    fun aivenKafkaTransactionManager(aivenProducerFactory: ProducerFactory<Nokkel, Beskjed>) =
        kafkaTransactionManager(aivenProducerFactory, kafkaClusterProperties.aiven)

    @Bean
    fun aivenKafkaJsonListenerContainerFactory(
        aivenConsumerFactory: ConsumerFactory<String, String>,
        aivenKafkaTemplate: KafkaTemplate<Nokkel, Beskjed>,
        aivenKafkaTransactionManager: KafkaTransactionManager<Nokkel, Beskjed>
    ): ConcurrentKafkaListenerContainerFactory<String, String> = configureConcurrentKafkaListenerContainerFactory(
        consumerFactory = aivenConsumerFactory,
        kafkaTemplate = aivenKafkaTemplate,
        retryInterval = kafkaClusterProperties.aiven.consumer.retryInterval,
        objectMapper = objectMapper,
        logger = logger,
        transactionManager = aivenKafkaTransactionManager
    )
}