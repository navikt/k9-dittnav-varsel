package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class K9Utkast @JsonCreator constructor(
    @JsonProperty("metadata") val metadata: Metadata,
    @JsonProperty("ytelse") val ytelse: Ytelse,
    @JsonProperty("utkast") val utkast: Map<String, Any?>,
)
