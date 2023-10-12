package no.nav.sifinnsynapi.konsumenter

import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_MICROFRONTEND
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_UTKAST
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.tms.microfrontend.Action
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class Konsument(
    private val dittnavService: DittnavService,
) {
    private val logger = LoggerFactory.getLogger(Konsument::class.java)

    @KafkaListener(
        topics = [K9_DITTNAV_VARSEL_BESKJED],
        id = "k9-dittnav-varsel-beskjed-aiven-listener",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        containerFactory = "beskjedKafkaJsonListenerContainerFactory"
    )
    fun konsumerBeskjed(@Payload melding: K9Beskjed) {
        if (melding.ytelse != null) logger.info(
            "Mottok K9Beskjed fra ytelse {} med eventID: {}, event: :{}",
            melding.ytelse,
            melding.eventId,
            melding
        ) else logger.info("Mottok K9Beskjed med eventID: {}, event: :{}", melding.eventId, melding)

        dittnavService.sendBeskjed(melding.somNøkkel(), melding.somBeskjed())
    }

    @KafkaListener(
        topics = [K9_DITTNAV_VARSEL_UTKAST],
        id = "k9-dittnav-varsel-utkast-aiven-listener",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        containerFactory = "utkastKafkaJsonListenerContainerFactory"
    )
    fun konsumerUtkst(@Payload utkast: K9Utkast) {
        val utkastJson = JSONObject(utkast.utkast)
        val utkastId = utkastJson.getString("utkastId")
        logger.info("Mottok K9Utkast fra ytelse {} med utkastId: {}", utkast.ytelse, utkastId)

        dittnavService.sendUtkast(utkastId, utkastJson.toString())
    }

    @KafkaListener(
        topics = [K9_DITTNAV_VARSEL_MICROFRONTEND],
        id = "k9-dittnav-varsel-microfrontend-aiven-listener",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        clientIdPrefix = "microfrontend-",
        containerFactory = "microfrontendKafkaJsonListenerContainerFactory",
    )
    fun konsumerMikrofrontendEvent(@Payload k9Microfrontend: K9Microfrontend) {
        logger.info("Mottok microfrontend event: {}", k9Microfrontend)

        val microfrontendMessage = buildMicrofrontendMessage(k9Microfrontend)

        dittnavService.toggleMicrofrontend(k9Microfrontend.metadata.correlationId, microfrontendMessage.text())
    }

    private fun buildMicrofrontendMessage(k9Microfrontend: K9Microfrontend): Action {
       return when (k9Microfrontend.action) {
            MicrofrontendAction.ENABLE -> MicrofrontendMessageBuilder.enable {
                ident = k9Microfrontend.ident
                initiatedBy = k9Microfrontend.initiatedBy
                microfrontendId = k9Microfrontend.microfrontendId.id
                sensitivitet = k9Microfrontend.sensitivitet!! // sensitivitet er påkrevd for ENABLE
            }

            MicrofrontendAction.DISABLE -> MicrofrontendMessageBuilder.disable {
                ident = k9Microfrontend.ident
                initiatedBy = k9Microfrontend.initiatedBy
                microfrontendId = k9Microfrontend.microfrontendId.id
            }
        }
    }
}
