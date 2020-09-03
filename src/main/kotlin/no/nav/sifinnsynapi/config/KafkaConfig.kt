package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.kafka.support.converter.JsonMessageConverter
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConfig(
        @Value("\${spring.kafka.consumer.retry-interval}")
        val retryInterval: Long,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection") val kafkaTemplate: KafkaTemplate<String, Any>,
        val objectMapper: ObjectMapper) {
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
        factory.setErrorHandler(SeekToCurrentErrorHandler(FixedBackOff(retryInterval, FixedBackOff.UNLIMITED_ATTEMPTS)))

        return factory
    }
}
