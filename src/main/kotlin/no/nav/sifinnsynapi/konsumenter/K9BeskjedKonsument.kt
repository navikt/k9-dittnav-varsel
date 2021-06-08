package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED_ONPREM
import no.nav.sifinnsynapi.dittnav.DittnavService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class InnsynHendelseKonsument(
    private val dittnavService: DittnavService,
    @Value("\${no.nav.sts.username}") private val stsUsername: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(InnsynHendelseKonsument::class.java)
    }

    @KafkaListener(
        topics = [K9_DITTNAV_VARSEL_BESKJED_ONPREM],
        id = "k9-dittnav-varsel-beskjed-onprem-listener",
        groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
        containerFactory = "onpremKafkaJsonListenerContainerFactory"
    )
    fun konsumer(@Payload melding: K9Beskjed) {
        logger.info("Mottok hendelse fra innsyn med eventID: {}, event: :{}", melding.eventId, melding)

        dittnavService.sendBeskjed(melding.somNøkkel(stsUsername), melding.somBeskjed())
    }
}