package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED_AIVEN
import no.nav.sifinnsynapi.utils.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
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

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
    topics = [K9_DITTNAV_VARSEL_BESKJED_AIVEN, DITT_NAV_BESKJED],
    count = 3,
    bootstrapServersProperty = "kafka-servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class AivenK9BeskjedKonsumentIntegrasjonsTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger.
    lateinit var dittNavConsumer: Consumer<Nokkel, Beskjed> // Kafka consumer som brukes til å lese kafka meldinger.

    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer()
        dittNavConsumer = embeddedKafkaBroker.opprettDittnavConsumer()
    }

    @AfterAll
    internal fun tearDown() {
        producer.close()
        dittNavConsumer.close()
    }

    @Test
    fun `Legger K9Beskjed på topic og forvent publisert dittnav beskjed`() {
        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt din melding om registrering av aleneomsorg.",
            link = "https://www.nav.no",
            ytelse = Ytelse.OMSORGSDAGER_ALENEOMSORG
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED_AIVEN, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon = dittNavConsumer.hentBrukernotifikasjon(k9Beskjed.eventId)?.value()
        validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
    }

    @Test
    fun `Legger K9Beskjed på topic fra ettersending og forvent publisert dittnav beskjed`() {
        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt din ettersendelse til pleiepenger.",
            link = null,
            ytelse = Ytelse.ETTERSENDING_PLEIEPENGER_SYKT_BARN
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED_AIVEN, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon = dittNavConsumer.hentBrukernotifikasjon(k9Beskjed.eventId)?.value()
        validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
    }

    @Test
    fun `Legger K9Beskjed på topic fra omsorgspenger utvidet rett og forvent publisert dittnav beskjed`() {
        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt søknad fra deg om ekstra omsorgsdager ved kronisk sykt eller funksjonshemmet barn.",
            link = null,
            ytelse = Ytelse.OMSORGSPENGER_UTV_KS
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED_AIVEN, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon = dittNavConsumer.hentBrukernotifikasjon(k9Beskjed.eventId)?.value()
        validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
    }

    @Test
    fun `Legger K9Beskjed på topic fra omsorgspenger utbetaling snf og forvent publisert dittnav beskjed`() {
        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Søknad om utbetaling av omsorgspenger er mottatt.",
            link = null,
            ytelse = Ytelse.OMSORGSPENGER_UT_SNF
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED_AIVEN, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon = dittNavConsumer.hentBrukernotifikasjon(k9Beskjed.eventId)?.value()
        validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
    }

    @Test
    fun `Legger K9Beskjed på topic fra pleiepenger livets sluttfase og forventer publisert dittnav beskjed`() {
        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Søknad om pleiepenger livets sluttfase",
            link = null,
            ytelse = Ytelse.PLEIEPENGER_LIVETS_SLUTTFASE
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED_AIVEN, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon = dittNavConsumer.hentBrukernotifikasjon(k9Beskjed.eventId)?.value()
        validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
    }
}
