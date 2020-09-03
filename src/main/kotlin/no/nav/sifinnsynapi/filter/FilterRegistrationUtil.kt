package no.nav.sifinnsynapi.filter

import java.util.*
import java.util.stream.Collectors

internal object FilterRegistrationUtil {
    private const val ALWAYS = "/*"
    fun urlPatternsFor(vararg patterns: String): List<String> {
        return Arrays.stream(patterns)
                .map { pattern: String -> pattern + ALWAYS }
                .collect(Collectors.toList())
    }

    fun always(): List<String> {
        return listOf(ALWAYS)
    }
}
