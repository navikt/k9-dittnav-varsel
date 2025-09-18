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
    val ytelse: String? = null
) {
    override fun toString(): String {
        return "K9Beskjed(metadata=$metadata, grupperingsId='$grupperingsId', tekst='$tekst', link='$link', dagerSynlig=$dagerSynlig, eventId='$eventId')"
    }
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
