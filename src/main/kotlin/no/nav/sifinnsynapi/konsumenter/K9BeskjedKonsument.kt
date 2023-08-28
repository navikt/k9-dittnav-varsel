package no.nav.sifinnsynapi.konsumenter

import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_UTKAST
import no.nav.sifinnsynapi.dittnav.DittnavService
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class K9BeskjedKonsument(
    private val dittnavService: DittnavService,
) {
    private val logger = LoggerFactory.getLogger(K9BeskjedKonsument::class.java)

    @KafkaListener(
        topics = [K9_DITTNAV_VARSEL_BESKJED],
        id = "k9-dittnav-varsel-beskjed-aiven-listener",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        containerFactory = "beskjedKafkaJsonListenerContainerFactory"
    )
    fun konsumerAiven(@Payload melding: K9Beskjed) {
        if (melding.ytelse != null) logger.info(
            "AIVEN - Mottok K9Beskjed fra ytelse {} med eventID: {}, event: :{}",
            melding.ytelse,
            melding.eventId,
            melding
        ) else logger.info("AIVEN - Mottok K9Beskjed med eventID: {}, event: :{}", melding.eventId, melding)

        dittnavService.sendBeskjed(melding.somNøkkel(), melding.somBeskjed())
    }

    @KafkaListener(
        topics = [K9_DITTNAV_VARSEL_UTKAST],
        id = "k9-dittnav-varsel-utkast-aiven-listener",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        containerFactory = "utkastKafkaJsonListenerContainerFactory"
    )
    fun konsumerUtkst(@Payload utkast: K9Utkast) {
        val utkastId = JSONObject(utkast.utkast).getString("utkastId")
        logger.info("DEBUG: {}", utkast.utkast) // TODO: Fjern før produksjon
        logger.info("Mottok K9Utkast fra ytelse {} med utkastId: {}", utkast.ytelse, utkastId)

        dittnavService.sendUtkast(utkastId, utkast.utkast)
    }
}
