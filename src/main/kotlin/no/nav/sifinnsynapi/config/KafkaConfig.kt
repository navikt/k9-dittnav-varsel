package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.configureConcurrentKafkaListenerContainerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.consumerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.stringProducerFactory
import no.nav.sifinnsynapi.konsumenter.K9Beskjed
import no.nav.sifinnsynapi.konsumenter.K9Microfrontend
import no.nav.sifinnsynapi.konsumenter.K9Utkast
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaConfig(
    private val objectMapper: ObjectMapper,
    private val kafkaClusterProperties: KafkaClusterProperties,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> = consumerFactory(kafkaClusterProperties.aiven)

    @Bean
    fun producerFactory(): ProducerFactory<String, String> = stringProducerFactory(kafkaClusterProperties.aiven)

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> =
        CommonKafkaConfig.kafkaTemplate(producerFactory)

    @Bean
    fun beskjedKafkaJsonListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
        kafkaTemplate: KafkaTemplate<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        configureConcurrentKafkaListenerContainerFactory<K9Beskjed>(
            consumerFactory = consumerFactory,
            kafkaTemplate = kafkaTemplate,
            retryInterval = kafkaClusterProperties.aiven.consumer.retryInterval,
            objectMapper = objectMapper,
            logger = logger,
            correlationIdExtractor = { it.metadata.correlationId }
        )

    @Bean
    fun utkastKafkaJsonListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
        kafkaTemplate: KafkaTemplate<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        configureConcurrentKafkaListenerContainerFactory<K9Utkast>(
            consumerFactory = consumerFactory,
            kafkaTemplate = kafkaTemplate,
            retryInterval = kafkaClusterProperties.aiven.consumer.retryInterval,
            objectMapper = objectMapper,
            logger = logger,
            correlationIdExtractor = { it.metadata.correlationId }
        )

    @Bean
    fun microfrontendKafkaJsonListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
        kafkaTemplate: KafkaTemplate<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        configureConcurrentKafkaListenerContainerFactory<K9Microfrontend>(
            consumerFactory = consumerFactory,
            kafkaTemplate = kafkaTemplate,
            retryInterval = kafkaClusterProperties.aiven.consumer.retryInterval,
            objectMapper = objectMapper,
            logger = logger,
            correlationIdExtractor = { it.metadata.correlationId }
        )
}
