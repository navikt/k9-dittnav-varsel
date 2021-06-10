package no.nav.sifinnsynapi.util

import org.slf4j.MDC
import java.util.*

object MDCUtil {
    @JvmOverloads
    fun toMDC(key: String?, value: String?, defaultValue: String? = null) {
        MDC.put(key, Optional.ofNullable(value)
                .orElse(defaultValue))
    }
}
