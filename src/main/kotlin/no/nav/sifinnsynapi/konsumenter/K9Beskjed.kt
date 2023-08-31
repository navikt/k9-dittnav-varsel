package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

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
    OMSORGSPENGER_MIDLERTIDIG_ALENE,
    OMSORGSDAGER_MELDING_OVERFØRE,
    OMSORGSDAGER_MELDING_KORONA,
    OMSORGSDAGER_MELDING_FORDELE,
    ETTERSENDING_PLEIEPENGER_SYKT_BARN,
    ETTERSENDING_PLEIEPENGER_LIVETS_SLUTTFASE,
    ETTERSENDING_OMP,
    ETTERSENDING_OMP_UTV_KS, // Ettersending - Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    ETTERSENDING_OMP_UT_SNF, // Ettersending - Omsorgspenger utbetaling SNF ytelse.
    ETTERSENDING_OMP_UT_ARBEIDSTAKER, // Ettersending - Omsorgspenger utbetaling arbeidstaker ytelse.
    ETTERSENDING_OMP_UTV_MA, // Ettersending - Omsorgspenger utvidet rett - midlertidig alene
    ETTERSENDING_OMP_DELE_DAGER, // Ettersending - Melding om deling av omsorgsdager,
    @Deprecated("Utgår") OMSORGSPENGER_UTV_KS, // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    OMSORGSPENGER_UTVIDET_RETT, // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    @Deprecated("Utgår") OMSORGSPENGER_UT_SNF, // Omsorgspenger utbetaling snf
    OMSORGSPENGER_UTBETALING_SNF, // Omsorgspenger utbetaling snf
    @Deprecated("Utgår") OMSORGSPENGER_UT_ARBEIDSTAKER, // Omsorgspenger utbetaling arbeidstaker,
    OMSORGSPENGER_UTBETALING_ARBEIDSTAKER,
    PLEIEPENGER_LIVETS_SLUTTFASE,
    PLEIEPENGER_SYKT_BARN,
    ENDRINGSMELDING_PLEIEPENGER_SYKT_BARN
}

data class Metadata @JsonCreator constructor(
    @JsonProperty("version") val version : Int,
    @JsonProperty("correlationId") val correlationId : String,
    @JsonProperty("requestId") val requestId : String? = null
)

fun <T> T.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

fun K9Beskjed.somNøkkel(): NokkelInput {
    return NokkelInputBuilder()
        .withAppnavn("k9-dittnav-varsel")
        .withNamespace("dusseldorf")
        .withFodselsnummer(søkerFødselsnummer)
        .withGrupperingsId(grupperingsId)
        .withEventId(eventId)
        .build()
}

fun K9Beskjed.somBeskjed(): BeskjedInput {
    val builder = BeskjedInputBuilder()
        .withEksternVarsling(false)
        .withSikkerhetsnivaa(4)
        .withSynligFremTil(LocalDateTime.now().plusDays(dagerSynlig))
        .withTekst(tekst)
        .withTidspunkt(LocalDateTime.now(UTC))

    if(link != null) builder.withLink(URL(link))
    return builder.build()
}
