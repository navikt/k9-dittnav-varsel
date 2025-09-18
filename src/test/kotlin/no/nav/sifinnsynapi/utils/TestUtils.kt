package no.nav.sifinnsynapi.utils

import no.nav.sifinnsynapi.konsumenter.K9Beskjed
import no.nav.sifinnsynapi.konsumenter.Metadata
import no.nav.sifinnsynapi.konsumenter.MicrofrontendAction
import no.nav.sifinnsynapi.konsumenter.MicrofrontendId
import no.nav.tms.microfrontend.Sensitivitet
import java.util.*

fun gyldigK9Beskjed(tekst: String, link: String? = null, ytelse: String? = null): K9Beskjed {
    return K9Beskjed(
        metadata = Metadata(
            version = 1,
            correlationId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString()
        ),
        grupperingsId = UUID.randomUUID().toString(),
        eventId = UUID.randomUUID().toString(),
        søkerFødselsnummer = "12345678910",
        tekst = tekst,
        link = link,
        dagerSynlig = 7,
        ytelse = ytelse
    )
}

fun gyldigK9Utkast(utkastId: String, ytelse: String): String {
    //language=json
    return """
    {
      "metadata": {
        "correlationId": "${UUID.randomUUID()}",
        "version": 1
      },
      "ytelse": "$ytelse",
      "utkast": {
        "utkastId": "$utkastId",
        "@event_name": "created",
        "@origin": "k9-brukerdialog-cache",
        "ident": "12345678910",
        "tittel": "Søknad om pleiepenger for sykt barn",
        "tittel_i18n": "{}",
        "link": "https://www.nav.no/familie/sykdom-i-familien/soknad/pleiepenger",
        "metrics": "{}"
      }
    }
    """.trimIndent()
}

fun gyldigK9Microfrontend(
    correlationId: String = UUID.randomUUID().toString(),
    ident: String,
    microfrontendId: MicrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN,
    action: MicrofrontendAction,
    sensitivitet: Sensitivitet? = null,
    initiatedBy: String = "k9-dittnav-varsel",

    ): String {
    //language=json
    return """
    {
      "metadata": {
        "correlationId": "$correlationId",
        "version": 1
      },
      "ident": "$ident",
      "microfrontendId": "${microfrontendId.name}",
      "action": "${action.name}",
      "sensitivitet": ${sensitivitet?.let { "\"${it.name}\"" }},
      "initiatedBy": "$initiatedBy"
    }
    """.trimIndent()
}
