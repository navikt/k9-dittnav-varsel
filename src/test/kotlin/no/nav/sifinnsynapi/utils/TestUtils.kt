package no.nav.sifinnsynapi.utils

import no.nav.sifinnsynapi.konsumenter.K9Beskjed
import no.nav.sifinnsynapi.konsumenter.Metadata
import no.nav.sifinnsynapi.konsumenter.Ytelse
import java.util.*

fun gyldigK9Beskjed(tekst: String, link: String? = null, ytelse: Ytelse? = null): K9Beskjed {
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

fun gyldigK9Utkast(utkastId: String, ytelse: Ytelse): String {
    //language=json
    return """
    {
      "metadata": {
        "correlationId": "${UUID.randomUUID()}",
        "version": 1
      },
      "ytelse": "${ytelse.name}",
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
