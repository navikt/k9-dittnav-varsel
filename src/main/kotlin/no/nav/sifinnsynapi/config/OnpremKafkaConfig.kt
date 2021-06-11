package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.configureConcurrentKafkaListenerContainerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.consumerFactory
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.kafkaTemplate
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.producerFactory
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
import org.springframework.kafka.transaction.KafkaTransactionManager


@Configuration
class OnpremKafkaConfig(
    private val objectMapper: ObjectMapper,
    private val kafkaClusterProperties: KafkaClusterProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(OnpremKafkaConfig::class.java)
    }

    @Bean
    fun onpremConsumerFactory(): ConsumerFactory<String, String> = consumerFactory(kafkaClusterProperties.onprem)

    @Bean
    fun onpremProducerFactory(): ProducerFactory<Nokkel, Beskjed> = producerFactory(kafkaClusterProperties.onprem)

    @Bean
    fun onpremKafkaTemplate(onpremProducerFactory: ProducerFactory<Nokkel, Beskjed>): KafkaTemplate<Nokkel, Beskjed> =
        kafkaTemplate(onpremProducerFactory, kafkaClusterProperties.onprem)

    @Bean
    fun onpremKafkaTransactionManager(onpremProducerFactory: ProducerFactory<Nokkel, Beskjed>) =
        CommonKafkaConfig.kafkaTransactionManager(onpremProducerFactory, kafkaClusterProperties.onprem)

    @Bean
    fun onpremKafkaJsonListenerContainerFactory(
        onpremConsumerFactory: ConsumerFactory<String, String>,
        onpremKafkaTemplate: KafkaTemplate<Nokkel, Beskjed>,
        onpremKafkaTransactionManager: KafkaTransactionManager<Nokkel, Beskjed>
    ): ConcurrentKafkaListenerContainerFactory<String, String> = configureConcurrentKafkaListenerContainerFactory(
        consumerFactory = onpremConsumerFactory,
        kafkaTemplate = onpremKafkaTemplate,
        retryInterval = kafkaClusterProperties.onprem.consumer.retryInterval,
        objectMapper = objectMapper,
        logger = logger,
        transactionManager = onpremKafkaTransactionManager
    )
}