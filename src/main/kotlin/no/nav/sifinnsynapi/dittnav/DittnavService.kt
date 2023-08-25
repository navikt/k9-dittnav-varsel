package no.nav.sifinnsynapi.dittnav

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_UTKAST
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class DittnavService(
    private val beskjedKafkaTemplate: KafkaTemplate<NokkelInput, BeskjedInput>,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    val logger = LoggerFactory.getLogger(DittnavService::class.java)

    fun sendBeskjed(nøkkel: NokkelInput, beskjed: BeskjedInput) {
        beskjedKafkaTemplate.send(ProducerRecord(DITT_NAV_BESKJED, nøkkel, beskjed))
            .exceptionally { ex: Throwable ->
                logger.warn(
                    "Kunne ikke sende melding {} på {}",
                    nøkkel.getEventId(),
                    DITT_NAV_BESKJED,
                    ex
                )
                throw ex
            }.thenAccept {
                logger.info("Sender beskjed videre til ${DITT_NAV_BESKJED} med eventId ${nøkkel.getEventId()}")
            }
    }

    fun sendUtkast(utkastId: String, utkast: String) {
        kafkaTemplate.send(ProducerRecord(DITT_NAV_UTKAST, utkastId,  utkast))
            .exceptionally { ex: Throwable ->
                logger.warn(
                    "Kunne ikke sende utkast til {}",
                    DITT_NAV_UTKAST,
                    ex
                )
                throw ex
            }.thenAccept {
                logger.info("Sender utkast videre til ${DITT_NAV_UTKAST}.")
            }
    }
}
