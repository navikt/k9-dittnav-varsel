package no.nav.sifinnsynapi.dittnav

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_VARSEL
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_MICROFRONTEND
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_UTKAST
import org.apache.kafka.clients.producer.ProducerRecord
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class DittnavService(
    private val beskjedKafkaTemplate: KafkaTemplate<NokkelInput, BeskjedInput>,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    val logger = LoggerFactory.getLogger(DittnavService::class.java)

    @Deprecated("Bruk sendVarsel istedet")
    fun sendBeskjed(nøkkel: NokkelInput, beskjed: BeskjedInput) {
        beskjedKafkaTemplate.send(ProducerRecord(DITT_NAV_BESKJED, nøkkel, beskjed))
            .exceptionally { ex: Throwable ->
                logger.warn("Kunne ikke sende melding {} på {}", nøkkel.getEventId(), DITT_NAV_BESKJED, ex)
                throw ex
            }.thenAccept {
                logger.info("Beskjed sendt til ${DITT_NAV_BESKJED} med eventId ${nøkkel.getEventId()}")
            }
    }

    fun sendVarsel(varselId: String, varselJson: String) {
        kafkaTemplate.send(ProducerRecord(DITT_NAV_VARSEL, varselId, varselJson))
            .exceptionally { ex: Throwable ->
                logger.warn("Kunne ikke sende varsel {} til {}", varselId, DITT_NAV_VARSEL, ex)
                throw ex
            }.thenAccept {
                logger.info("Varsel sendt til $DITT_NAV_VARSEL med varselId $varselId")
            }
    }

    fun sendUtkast(utkastId: String, utkast: String) {
        kafkaTemplate.send(ProducerRecord(DITT_NAV_UTKAST, utkastId,  utkast))
            .exceptionally { ex: Throwable ->
                logger.warn("Kunne ikke sende utkast til {}", DITT_NAV_UTKAST, ex)
                throw ex
            }.thenAccept {
                val anonymisertUtkast = JSONObject(it.producerRecord.value())
                anonymisertUtkast.remove("ident") // Fjerner ident fra utkastet før det logges.
                logger.info("Utkast sendt til ${DITT_NAV_UTKAST}. {}", anonymisertUtkast)
            }
    }

    fun toggleMicrofrontend(mikrofrontendId: String, mikrofrontend: String) {
        kafkaTemplate.send(ProducerRecord(DITT_NAV_MICROFRONTEND, mikrofrontendId,  mikrofrontend))
            .exceptionally { ex: Throwable ->
                logger.warn("Kunne ikke sende microfrontend event til {}", DITT_NAV_MICROFRONTEND, ex)
                throw ex
            }.thenAccept {
                val anonymisertUtkast = JSONObject(it.producerRecord.value())
                anonymisertUtkast.remove("ident") // Fjerner ident fra microfrontend event før det logges.
                logger.info("Microfrontend event sendt til ${DITT_NAV_MICROFRONTEND}. {}", anonymisertUtkast)
            }
    }
}
