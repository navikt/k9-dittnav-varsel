package no.nav.sifinnsynapi.innsyn

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.Topics.INNSYN_MOTTATT
import no.nav.sifinnsynapi.dittnav.DittnavService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
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

    @KafkaListener(topics = [INNSYN_MOTTATT], id = "innsyn-mottatt-listener", groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload melding: InnsynMelding) {
        logger.info("Mottok hendelse fra innsyn med eventID: {}", melding.eventId)

        dittnavService.sendBeskjed(
                melding.somNøkkel(stsUsername),
                melding.somBeskjed()
        )
    }
}

data class InnsynMelding(
        val grupperingsId: String,
        val tekst: String,
        val link: String,
        val dagerSynlig: Long,
        val søkerFødselsnummer: String,
        val eventId: String
)

fun InnsynMelding.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

private fun InnsynMelding.somNøkkel(systembruker: String): Nokkel {
    return Nokkel(
            systembruker,
            eventId
    )
}

private fun InnsynMelding.somBeskjed(): Beskjed {
    return Beskjed(
            System.currentTimeMillis(),
            Instant.now().plus(dagerSynlig, ChronoUnit.DAYS).toEpochMilli(),
            søkerFødselsnummer,
            grupperingsId,
            tekst,
            link,
            4
    )
}
