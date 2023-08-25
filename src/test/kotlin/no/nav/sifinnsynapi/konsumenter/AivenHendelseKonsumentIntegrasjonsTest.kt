package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.Topics
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_UTKAST
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_UTKAST
import no.nav.sifinnsynapi.utils.*
import no.nav.tms.utkast.builder.UtkastJsonBuilder
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.util.*
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
    topics = [K9_DITTNAV_VARSEL_BESKJED, DITT_NAV_BESKJED, K9_DITTNAV_VARSEL_UTKAST, DITT_NAV_UTKAST],
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
    lateinit var beskjedConsumer: Consumer<NokkelInput, BeskjedInput> // Kafka consumer som brukes til å lese kafka meldinger.
    lateinit var utkastConsumer: Consumer<String, String> // Kafka consumer som brukes til å lese utkaster.


    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer()
        beskjedConsumer =
            embeddedKafkaBroker.opprettKafkaConsumer(groupId = "beskjed-consumer", topicName = DITT_NAV_BESKJED)
        utkastConsumer =
            embeddedKafkaBroker.opprettKafkaConsumer(groupId = "utkast-consumer", topicName = DITT_NAV_UTKAST)
    }

    @AfterAll
    internal fun tearDown() {
        producer.close()
        beskjedConsumer.close()
        utkastConsumer.close()
    }

    @Test
    fun `Legger K9Beskjed på topic og forvent publisert dittnav beskjed`() {
        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt din melding om registrering av aleneomsorg.",
            link = "https://www.nav.no",
            ytelse = Ytelse.OMSORGSDAGER_ALENEOMSORG
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon =
            beskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon =
            beskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon =
            beskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon =
            beskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon =
            beskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
        validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
    }

    @Test
    fun `Legger K9Beskjed på topic fra omsorgspenger utbetaling arbeidstaker og forventer publisert dittnav beskjed`() {
        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Søknad om utbetaling av omsorgspenger for arbeidstaker",
            link = null,
            ytelse = Ytelse.OMSORGSPENGER_UT_ARBEIDSTAKER
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        // forvent at mottatt hendelse konsumeres og at det blir sendt ut en beskjed på aapen-brukernotifikasjon-nyBeskjed-v1 topic
        val brukernotifikasjon =
            beskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
        validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
    }

    @Test
    fun `Legger utkast på topic og forventer riktig dittnav utkast`() {

        // legg på 1 hendelse om mottatt søknad
        val utkastId = UUID.randomUUID().toString()
        val utkast = gyldigK9Utkast(
            UtkastJsonBuilder()
                .withUtkastId(utkastId)
                .withIdent("12345678910")
                .withTittel("Søknad om pleiepenger sykt barn")
                .withLink("https://www.nav.no/familie/sykdom-i-familien/soknad/pleiepenger/soknad")
                .create(), Ytelse.PLEIEPENGER_SYKT_BARN
        )

        producer.leggPåTopic(utkast, Topics.K9_DITTNAV_VARSEL_UTKAST, mapper)

        val konsumertUtkast = utkastConsumer.hentMelding(DITT_NAV_UTKAST) { it == utkastId }?.value()
        validerRiktigUtkast(utkast.utkast, konsumertUtkast)
    }
}


fun validerRiktigBrukernotifikasjon(k9Beskjed: K9Beskjed, brukernotifikasjon: BeskjedInput?) {
    assertTrue(brukernotifikasjon != null)
    assertTrue(k9Beskjed.tekst == brukernotifikasjon?.getTekst())
    k9Beskjed.link?.let { assertTrue(k9Beskjed.link == brukernotifikasjon?.getLink()) }
}

fun validerRiktigUtkast(utkast: String, konsumertUtkast: String?) {
    assertTrue(konsumertUtkast != null)
    assertTrue(utkast == konsumertUtkast)
}
