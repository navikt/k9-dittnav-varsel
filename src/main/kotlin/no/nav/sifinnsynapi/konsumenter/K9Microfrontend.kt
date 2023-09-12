package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tms.microfrontend.Sensitivitet

data class K9Microfrontend @JsonCreator constructor(
    @JsonProperty("metadata") val metadata: Metadata,
    @JsonProperty("ident") val ident: String,
    @JsonProperty("microfrontendId") val microfrontendId: MicrofrontendId,
    @JsonProperty("action") val action: MicrofrontendAction,
    @JsonProperty("sensitivitet") val sensitivitet: Sensitivitet? = null,
    @JsonProperty("initiatedBy") val initiatedBy: String,
) {
    override fun toString(): String {
        return "K9Microfrontend(metadata=$metadata, ident='***********', microfrontendId='$microfrontendId', toggle=$action, sensitivitet=$sensitivitet)"
    }
}

enum class MicrofrontendAction {
    ENABLE, DISABLE
}

/**
 * MicrofrontendId avtales på forhånd med team-personbruker.
 */
enum class MicrofrontendId(val id: String) {
    PLEIEPENGER_INNSYN("pleiepenger-innsyn"),
}
