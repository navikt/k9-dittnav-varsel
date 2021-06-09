package no.nav.sifinnsynapi.konsumenter

import assertk.assertThat
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED_ONPREM
import no.nav.sifinnsynapi.utils.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
        topics = [K9_DITTNAV_VARSEL_BESKJED_ONPREM, DITT_NAV_BESKJED],
        bootstrapServersProperty = "kafka.onprem.servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class OnpremInnsynHendelseKonsumentIntegrasjonsTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om mottat søknad i innsyn.
    lateinit var dittNavConsumer: Consumer<Nokkel, Beskjed> // Kafka consumer som brukes til å lese kafka meldinger.

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OnpremInnsynHendelseKonsumentIntegrasjonsTest::class.java)
    }

    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer()
        dittNavConsumer = embeddedKafkaBroker.opprettDittnavConsumer()
    }

    @AfterEach
    internal fun tearDown() {
    }

    @Test
    fun `gitt konsumert hendelse, forvent publisert dittnav beskjed`() {
        // legg på 1 hendelse om mottatt søknad om pleiepenger sykt barn...
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt din søknad om pleiepenger - sykt barn. Klikk under for mer info.",
            grupperingsId = "pleiepenger-sykt-barn",
            link = "https://www.nav.no"
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED_ONPREM, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        await.atMost(60, TimeUnit.SECONDS).untilAsserted {
            val brukernotifikasjon = dittNavConsumer.hentBrukernotifikasjon(k9Beskjed.eventId)?.value()
            if(brukernotifikasjon != null) validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
        }
    }

    @Test
    fun `Skal håndtere at link i K9Beskjed er satt til null, gjør den om til ""`() {
        // legg på 1 hendelse om mottatt søknad om midlertidig alene med link = null...
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt omsorgspengesøknad fra deg om å bli regnet som alene om omsorgen for barn.",
            grupperingsId = "omp-midlertidig-alene",
            link = null
        )
        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED_ONPREM, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        await.atMost(60, TimeUnit.SECONDS).untilAsserted {
            val brukernotifikasjon = dittNavConsumer.hentBrukernotifikasjon(k9Beskjed.eventId)?.value()
            if(brukernotifikasjon != null) validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
        }
    }
}

fun validerRiktigBrukernotifikasjon(k9Beskjed: K9Beskjed, brukernotifikasjon: Beskjed){
    assertThat(k9Beskjed.tekst == brukernotifikasjon.getTekst())
    assertThat(k9Beskjed.søkerFødselsnummer == brukernotifikasjon.getFodselsnummer())
    assertThat(k9Beskjed.grupperingsId == brukernotifikasjon.getGrupperingsId())
}