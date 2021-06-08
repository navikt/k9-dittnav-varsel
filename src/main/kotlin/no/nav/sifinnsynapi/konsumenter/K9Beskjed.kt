package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import java.time.Instant
import java.time.temporal.ChronoUnit

data class K9Beskjed(
    val metadata: Metadata,
    val grupperingsId: String,
    val tekst: String,
    val link: String? = null,
    val dagerSynlig: Long,
    val søkerFødselsnummer: String,
    val eventId: String
) {
    override fun toString(): String {
        return "K9Beskjed(metadata=$metadata, grupperingsId='$grupperingsId', tekst='$tekst', link='$link', dagerSynlig=$dagerSynlig, søkerFødselsnummer='***********', eventId='$eventId')"
    }
}

data class Metadata @JsonCreator constructor(
    @JsonProperty("version") val version : Int,
    @JsonProperty("correlationId") val correlationId : String,
    @JsonProperty("requestId") val requestId : String
)

fun K9Beskjed.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

fun K9Beskjed.somNøkkel(systembruker: String): Nokkel {
    return Nokkel(
        systembruker,
        eventId
    )
}

fun K9Beskjed.somBeskjed(): Beskjed {
    val linkUtenNull = link ?: ""
    return Beskjed(
        System.currentTimeMillis(),
        Instant.now().plus(dagerSynlig, ChronoUnit.DAYS).toEpochMilli(),
        søkerFødselsnummer,
        grupperingsId,
        tekst,
        linkUtenNull,
        4
    )
}