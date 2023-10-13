package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_MICROFRONTEND
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_UTKAST
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_MICROFRONTEND
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_UTKAST
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.utils.gyldigK9Beskjed
import no.nav.sifinnsynapi.utils.gyldigK9Microfrontend
import no.nav.sifinnsynapi.utils.gyldigK9Utkast
import no.nav.sifinnsynapi.utils.hentMelding
import no.nav.sifinnsynapi.utils.leggPåTopic
import no.nav.sifinnsynapi.utils.opprettKafkaAvroConsumer
import no.nav.sifinnsynapi.utils.opprettKafkaProducer
import no.nav.tms.microfrontend.Sensitivitet
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
    topics = [K9_DITTNAV_VARSEL_BESKJED, DITT_NAV_BESKJED, K9_DITTNAV_VARSEL_UTKAST, DITT_NAV_UTKAST, K9_DITTNAV_VARSEL_MICROFRONTEND, DITT_NAV_MICROFRONTEND],
    count = 3,
    bootstrapServersProperty = "kafka-servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class KafkaErrorHandlerTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger
    lateinit var beskjedConsumer: Consumer<NokkelInput, BeskjedInput> // Kafka consumer som brukes til å lese kafka meldinger.
    lateinit var utkastConsumer: Consumer<String, String> // Kafka consumer som brukes til å lese utkaster.

    @MockkBean()
    lateinit var dittnavService: DittnavService

    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer()
        beskjedConsumer =
            embeddedKafkaBroker.opprettKafkaAvroConsumer(groupId = "beskjed-consumer", topicName = DITT_NAV_BESKJED)
        utkastConsumer =
            embeddedKafkaBroker.opprettKafkaAvroConsumer(groupId = "utkast-consumer", topicName = DITT_NAV_UTKAST)
    }

    @AfterAll
    internal fun tearDown() {
        producer.close()
        beskjedConsumer.close()
        utkastConsumer.close()
    }

    @Test
    fun `Sender K9Beskjed hvor dittnavService feiler, forvent at SeekToCurrentErrorHandler prøver igjen minst 10 ganger`() {
        mockDittnavServiceFailure()

        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt din søknad om pleiepenger - sykt barn. Klikk under for mer info.",
            link = "https://www.nav.no"
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        awaitAndAssertNull {
            beskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
        }

        verify(atLeast = 10) {
            dittnavService.sendBeskjed(any(), any())
        }
    }

    @Test
    fun `Sender utkast hvor dittnavService feiler, forvent at SeekToCurrentErrorHandler prøver igjen minst 10 ganger`() {
        mockDittnavServiceUtkastFailure()

        val utkastId = UUID.randomUUID().toString()

        producer.leggPåTopic(gyldigK9Utkast(utkastId, Ytelse.PLEIEPENGER_SYKT_BARN), K9_DITTNAV_VARSEL_UTKAST, mapper)

        awaitAndAssertNull { utkastConsumer.hentMelding(DITT_NAV_UTKAST) { it == utkastId }?.value() }

        verify(atLeast = 10) {
            dittnavService.sendUtkast(any(), any())
        }
    }

    @Test
    fun `Sender microfrontend event hvor dittnavService feiler, forvent at SeekToCurrentErrorHandler prøver igjen minst 10 ganger`() {
        mockDittnavServiceUtkastFailure()

        val correlationId = UUID.randomUUID().toString()

        producer.leggPåTopic(gyldigK9Microfrontend(
            correlationId = correlationId,
            ident = "12345678910",
            action = MicrofrontendAction.ENABLE,
            sensitivitet = Sensitivitet.HIGH,
        ), K9_DITTNAV_VARSEL_MICROFRONTEND, mapper)

        awaitAndAssertNull { utkastConsumer.hentMelding(DITT_NAV_MICROFRONTEND) { it == correlationId }?.value() }

        verify(atLeast = 10) {
            dittnavService.toggleMicrofrontend(any(), any())
        }
    }

    private fun mockDittnavServiceFailure() {
        every { dittnavService.sendBeskjed(any(), any()) } throws Exception(MOCKED_ERROR_MESSAGE)
    }

    private fun mockDittnavServiceUtkastFailure() {
        every { dittnavService.sendUtkast(any(), any()) } throws Exception(MOCKED_ERROR_MESSAGE)
    }

    private fun awaitAndAssertNull(valueProvider: () -> Any?) {
        await.atMost(30, TimeUnit.SECONDS).untilAsserted {
            assertTrue(valueProvider.invoke() == null, "Expected value to be null after waiting but it was not.")
        }
    }

    companion object {
        const val MOCKED_ERROR_MESSAGE = "Ops noe gikk galt! Mocket feil"
    }
}
