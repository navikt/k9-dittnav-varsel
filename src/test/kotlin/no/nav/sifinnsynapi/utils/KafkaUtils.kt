package no.nav.sifinnsynapi.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED_AIVEN
import no.nav.sifinnsynapi.konsumenter.K9Beskjed
import no.nav.sifinnsynapi.konsumenter.somJson
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration

fun EmbeddedKafkaBroker.opprettKafkaProducer(): Producer<String, Any> {
    return DefaultKafkaProducerFactory<String, Any>(HashMap(KafkaTestUtils.producerProps(this))).createProducer()
}

fun Producer<String, Any>.leggPåTopic(hendelse: K9Beskjed, topic: String, mapper: ObjectMapper) {
    this.send(ProducerRecord(topic, hendelse.somJson(mapper)))
    this.flush()
}

fun EmbeddedKafkaBroker.opprettDittnavConsumer(): Consumer<NokkelInput, BeskjedInput> {
    val consumerProps = KafkaTestUtils.consumerProps("dittnv-consumer", "true", this)
    consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = "io.confluent.kafka.serializers.KafkaAvroDeserializer"
    consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = "io.confluent.kafka.serializers.KafkaAvroDeserializer"
    consumerProps[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = "true"
    consumerProps[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://localhost"

    val consumer = DefaultKafkaConsumerFactory<NokkelInput, BeskjedInput>(HashMap(consumerProps)).createConsumer()
    consumer.subscribe(listOf(DITT_NAV_BESKJED_AIVEN))
    return consumer
}

fun Consumer<NokkelInput, BeskjedInput>.hentBrukernotifikasjon(søknadId: String): ConsumerRecord<NokkelInput, BeskjedInput>? {
    val end = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis()
    seekToBeginning(assignment())
    while (System.currentTimeMillis() < end) {

        val entries: List<ConsumerRecord<NokkelInput, BeskjedInput>> = poll(Duration.ofSeconds(5))
            .records(DITT_NAV_BESKJED_AIVEN)
            .filter { it.key().getEventId() == søknadId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first()
        }
    }
    return null
    //throw IllegalStateException("Fant ikke dittnav varsel for søknad med id=$søknadId etter 20 sekunder.")
}
