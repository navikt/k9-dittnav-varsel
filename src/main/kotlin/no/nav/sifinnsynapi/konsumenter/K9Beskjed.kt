package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.ZoneId
import java.time.ZonedDateTime

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

enum class Ytelse {
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
    ETTERSENDING_OPPLARINGSPENGER, // Ettersending - Opplæringspenger
    @Deprecated("Utgår")
    OMSORGSPENGER_UTV_KS, // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    OMSORGSPENGER_UTVIDET_RETT, // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    @Deprecated("Utgår")
    OMSORGSPENGER_UT_SNF, // Omsorgspenger utbetaling snf
    OMSORGSPENGER_UTBETALING_SNF, // Omsorgspenger utbetaling snf
    @Deprecated("Utgår")
    OMSORGSPENGER_UT_ARBEIDSTAKER, // Omsorgspenger utbetaling arbeidstaker,
    OMSORGSPENGER_UTBETALING_ARBEIDSTAKER,
    PLEIEPENGER_LIVETS_SLUTTFASE,
    PLEIEPENGER_SYKT_BARN,
    ENDRINGSMELDING_PLEIEPENGER_SYKT_BARN,
    OPPLARINGSPENGER, // Opplæringspenger
    UNGDOMSYTELSE; // Ungdomsytelse
    ;
}

data class Metadata @JsonCreator constructor(
    @JsonProperty("version") val version: Int,
    @JsonProperty("correlationId") val correlationId: String,
    @JsonProperty("requestId") val requestId: String? = null
)

fun <T> T.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

fun K9Beskjed.somVarselOpprett(): String {
    return VarselActionBuilder.opprett {
        type = Varseltype.Beskjed
        varselId = eventId
        sensitivitet = Sensitivitet.High // sikkerhetsnivå 4
        ident = søkerFødselsnummer
        tekster += Tekst(
            spraakkode = "nb",
            tekst = this@somVarselOpprett.tekst,
            default = true
        )
        link = this@somVarselOpprett.link
        aktivFremTil = ZonedDateTime.now(ZoneId.of("Z")).plusDays(dagerSynlig)
        produsent = Produsent (
            cluster = System.getenv("NAIS_CLUSTER_NAME") ?: "prod-gcp",
            namespace = System.getenv("NAIS_NAMESPACE") ?: "dusseldorf",
            appnavn = System.getenv("NAIS_APP_NAME") ?: "k9-dittnav-varsel"
        )
    }
}
