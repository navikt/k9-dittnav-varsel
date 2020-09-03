package no.nav.sifinnsynapi.filter

import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.stereotype.Component

@Component
class HeadersToMDCFilterRegistrationBean(headersFilter: HeadersToMDCFilterBean?) : FilterRegistrationBean<HeadersToMDCFilterBean?>() {
    companion object {
        private val logger = LoggerFactory.getLogger(HeadersToMDCFilterRegistrationBean::class.java)
    }

    init {
        filter = headersFilter
        urlPatterns = FilterRegistrationUtil.always()
        logger.info("Registrert filter {}", this.javaClass.simpleName)
    }
}
