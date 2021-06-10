package no.nav.sifinnsynapi.filter

import no.nav.sifinnsynapi.util.CallIdGenerator
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class HeadersToMDCFilterBean(
        private val generator: CallIdGenerator,
        @Value("\${spring.application.name:k9-dittnav-varsel}") private val applicationName: String) : GenericFilterBean() {

    companion object {
        private val LOG = LoggerFactory.getLogger(HeadersToMDCFilterBean::class.java)
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        putValues(HttpServletRequest::class.java.cast(request))
        chain.doFilter(request, response)
    }

    private fun putValues(req: HttpServletRequest) {
        try {
            MDCUtil.toMDC(Constants.NAV_CONSUMER_ID, req.getHeader(Constants.NAV_CONSUMER_ID), applicationName)
            MDCUtil.toMDC(Constants.CORRELATION_ID, req.getHeader(Constants.CORRELATION_ID), generator.create())
        } catch (e: Exception) {
            LOG.warn("Feil ved setting av MDC-verdier for {}, MDC-verdier er inkomplette", req.requestURI, e)
        }
    }

    override fun toString(): String {
        return javaClass.simpleName + " [generator=" + generator + ", applicationName=" + applicationName + "]"
    }
}
