package no.nav.sifinnsynapi.dittnav

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class DittnavService(
    private val onpremKafkaTemplate: KafkaTemplate<Nokkel, Beskjed>
) {
    companion object{
        val logger = LoggerFactory.getLogger(DittnavService::class.java)
    }
    fun sendBeskjed(nøkkel: Nokkel, beskjed: Beskjed) {
        logger.info("Sender beskjed videre til ${DITT_NAV_BESKJED} med eventId ${nøkkel.getEventId()}")
        onpremKafkaTemplate.send(
            ProducerRecord(
                DITT_NAV_BESKJED,
                nøkkel,
                beskjed
            )
        )
    }
}
