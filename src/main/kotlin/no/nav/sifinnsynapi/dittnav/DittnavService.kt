package no.nav.sifinnsynapi.dittnav

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.Topics
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED_AIVEN
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class DittnavService(
    private val aivenKafkaTemplate: KafkaTemplate<NokkelInput, BeskjedInput>,
) {
    val logger = LoggerFactory.getLogger(DittnavService::class.java)

    fun sendBeskjedPåAiven(nøkkel: NokkelInput, beskjed: BeskjedInput) {
        aivenKafkaTemplate.send(
            ProducerRecord(
                DITT_NAV_BESKJED_AIVEN,
                nøkkel,
                beskjed
            )
        ).exceptionally { ex: Throwable ->
            logger.warn(
                "Kunne ikke sende melding {} på {}",
                nøkkel.getEventId(),
                Topics.K9_DITTNAV_VARSEL_BESKJED_AIVEN,
                ex
            )
            throw ex
        }.thenAccept {
                logger.info("Sender beskjed videre til ${DITT_NAV_BESKJED_AIVEN} med eventId ${nøkkel.getEventId()}")
            }
    }
}
