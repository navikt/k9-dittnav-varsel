package no.nav.sifinnsynapi.utils

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.konsumenter.somJson
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration

fun EmbeddedKafkaBroker.opprettKafkaProducer(): Producer<String, Any> {
    return DefaultKafkaProducerFactory<String, Any>(HashMap(KafkaTestUtils.producerProps(this))).createProducer()
}

fun <T> Producer<String, Any>.leggPåTopic(data: T, topic: String, mapper: ObjectMapper) {
    requireNotNull(data)
    if (data is String) {
        this.send(ProducerRecord(topic, data))
        this.flush()
        return
    }
    this.send(ProducerRecord(topic, data.somJson(mapper)))
    this.flush()
}

fun <K, V> EmbeddedKafkaBroker.opprettKafkaStringConsumer(groupId: String, topics: List<String>): Consumer<K, V> {

    val consumerProps = KafkaTestUtils.consumerProps(groupId, "true", this)
    consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

    val consumer = DefaultKafkaConsumerFactory<K, V>(HashMap(consumerProps)).createConsumer()
    consumer.subscribe(topics)
    return consumer
}

fun <K, V> Consumer<K, V>.hentMelding(
    topic: String,
    keyPredicate: (K) -> Boolean,
): ConsumerRecord<K, V>? {
    val end = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis()
    seekToBeginning(assignment())
    while (System.currentTimeMillis() < end) {
        val entries: List<ConsumerRecord<K, V>> = poll(Duration.ofSeconds(5))
            .records(topic)
            .filter { keyPredicate(it.key()) }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first()
        }
    }
    return null
}
