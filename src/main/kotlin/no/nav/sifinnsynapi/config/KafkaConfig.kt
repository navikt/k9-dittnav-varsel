package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.konsumenter.K9Beskjed
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.GenericErrorHandler
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.kafka.support.converter.JsonMessageConverter
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration

@Configuration
class KafkaConfig(
        @Value("\${spring.kafka.consumer.retry-interval}")
        val retryInterval: Long,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection") val kafkaTemplate: KafkaTemplate<String, Any>,
        val objectMapper: ObjectMapper,
        @Value("\${no.nav.sts.username}") val stsUsername: String,
        @Value("\${no.nav.sts.password}") val stsPassword: String,
        @Value("\${spring.application.name:k9-dittnav-varsel}") private val applicationName: String
) {


    init {
        logger.info("STS_USERNAME = {}", stsUsername)
        logger.info("STS_PASSWORD = {}", stsPassword)
    }

    companion object{
        private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)
    }

    @Bean
    fun kafkaJsonListenerContainerFactory(@Suppress("SpringJavaInjectionPointsAutowiringInspection") consumerFactory: ConsumerFactory<String, String>): KafkaListenerContainerFactory<*> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.setReplyTemplate(kafkaTemplate)
        factory.setMessageConverter(JsonMessageConverter(objectMapper))
        factory.containerProperties.isAckOnError = false;
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD;
        // https://docs.spring.io/spring-kafka/reference/html/#listener-container
        factory.containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(10L)

        val seekToCurrentErrorHandler = SeekToCurrentErrorHandler(FixedBackOff(retryInterval, FixedBackOff.UNLIMITED_ATTEMPTS))
        seekToCurrentErrorHandler.setClassifications(mapOf(), true)
        factory.setErrorHandler(seekToCurrentErrorHandler)
        factory.setRecordFilterStrategy {
            val melding = objectMapper.readValue(it.value(), K9Beskjed::class.java)
            val correlationId = melding.metadata.correlationId
            MDCUtil.toMDC(Constants.NAV_CALL_ID, correlationId)
            MDCUtil.toMDC(Constants.NAV_CONSUMER_ID, applicationName)

            false
        }
        return factory
    }
}
