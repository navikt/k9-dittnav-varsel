package no.nav.sifinnsynapi.dittnav

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class DittnavService(private val kafkaTemplate: KafkaTemplate<Nokkel, Beskjed>) {

    fun sendBeskjed(nøkkel: Nokkel, beskjed: Beskjed) {
        kafkaTemplate.send(ProducerRecord(
                DITT_NAV_BESKJED,
                nøkkel,
                beskjed
        ))
    }
}
