package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.BeskjedBuilder
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit

data class K9Beskjed(
    val metadata: Metadata,
    val grupperingsId: String,
    val tekst: String,
    val link: String? = null,
    val dagerSynlig: Long,
    val søkerFødselsnummer: String,
    val eventId: String,
    val ytelse: Ytelse? = null
) {
    override fun toString(): String {
        return "K9Beskjed(metadata=$metadata, grupperingsId='$grupperingsId', tekst='$tekst', link='$link', dagerSynlig=$dagerSynlig, eventId='$eventId')"
    }
}

enum class Ytelse{
    OMSORGSDAGER_ALENEOMSORG,
    OMSORGSPENGER_MIDLERTIDIG_ALENE
}

data class Metadata @JsonCreator constructor(
    @JsonProperty("version") val version : Int,
    @JsonProperty("correlationId") val correlationId : String,
    @JsonProperty("requestId") val requestId : String? = null
)

fun K9Beskjed.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

fun K9Beskjed.somNøkkel(systembruker: String): Nokkel {
    return Nokkel(
        systembruker,
        eventId
    )
}

fun K9Beskjed.somBeskjed(): Beskjed {
    val builder = BeskjedBuilder()
        .withEksternVarsling(false)
        .withFodselsnummer(this.søkerFødselsnummer)
        .withGrupperingsId(this.grupperingsId)
        .withSikkerhetsnivaa(4)
        .withSynligFremTil(LocalDateTime.now().plusDays(dagerSynlig))
        .withTekst(tekst)
        .withTidspunkt(LocalDateTime.now(UTC))

    if(link != null) builder.withLink(URL(link))
    return builder.build()
}