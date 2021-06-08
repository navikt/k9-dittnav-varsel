package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor
import org.springframework.kafka.support.converter.JsonMessageConverter
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration
import java.util.function.BiConsumer

class CommonKafkaConfig {
    companion object {
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
            factory.containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(10L)

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#delivery-header
            factory.containerProperties.isDeliveryAttemptHeader = true

            //https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#after-rollback
            val defaultAfterRollbackProcessor = DefaultAfterRollbackProcessor<String, String>(recoverer(logger), FixedBackOff(retryInterval, Long.MAX_VALUE))
            defaultAfterRollbackProcessor.setClassifications(mapOf(), true)
            factory.setAfterRollbackProcessor(defaultAfterRollbackProcessor)

            return factory
        }

        private fun recoverer(logger: Logger) = BiConsumer { cr: ConsumerRecord<*, *>, ex: Exception ->
            logger.error("Retry attempts exhausted for ${cr.topic()}-${cr.partition()}@${cr.offset()}", ex)
        }
    }
}
