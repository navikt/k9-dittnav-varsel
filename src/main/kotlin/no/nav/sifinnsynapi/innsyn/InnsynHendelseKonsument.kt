package no.nav.sifinnsynapi.innsyn

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
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

    @KafkaListener(topics = [K9_DITTNAV_VARSEL_BESKJED], id = "innsyn-mottatt-listener", groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload melding: InnsynMelding) {
        logger.info("Mottok hendelse fra innsyn med eventID: {}", melding.eventId)

        dittnavService.sendBeskjed(
                melding.somNøkkel(stsUsername),
                melding.somBeskjed()
        )
    }
}

data class InnsynMelding(
        val metadata: Metadata,
        val grupperingsId: String,
        val tekst: String,
        val link: String,
        val dagerSynlig: Long,
        val søkerFødselsnummer: String,
        val eventId: String
)

data class Metadata @JsonCreator constructor(
        @JsonProperty("version") val version : Int,
        @JsonProperty("correlationId") val correlationId : String,
        @JsonProperty("requestId") val requestId : String
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
