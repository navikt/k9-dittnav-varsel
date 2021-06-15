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