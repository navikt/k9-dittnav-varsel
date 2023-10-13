package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_MICROFRONTEND
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_UTKAST
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_MICROFRONTEND
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_UTKAST
import no.nav.sifinnsynapi.utils.gyldigK9Beskjed
import no.nav.sifinnsynapi.utils.gyldigK9Microfrontend
import no.nav.sifinnsynapi.utils.gyldigK9Utkast
import no.nav.sifinnsynapi.utils.hentMelding
import no.nav.sifinnsynapi.utils.leggPåTopic
import no.nav.sifinnsynapi.utils.opprettKafkaAvroConsumer
import no.nav.sifinnsynapi.utils.opprettKafkaProducer
import no.nav.sifinnsynapi.utils.opprettKafkaStringConsumer
import no.nav.tms.microfrontend.Sensitivitet
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

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
class KonsumentIntegrasjonsTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger.
    lateinit var dittnavBeskjedConsumer: Consumer<NokkelInput, BeskjedInput> // Kafka consumer som brukes til å lese beskjeder.
    lateinit var dittnavStringConsumer: Consumer<String, String> // Kafka consumer som brukes til å lese meldinger.

    private companion object {
        val logger = LoggerFactory.getLogger(KonsumentIntegrasjonsTest::class.java)
    }

    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer()
        dittnavBeskjedConsumer =
            embeddedKafkaBroker.opprettKafkaAvroConsumer(groupId = "beskjed-consumer", topicName = DITT_NAV_BESKJED)
        dittnavStringConsumer =
            embeddedKafkaBroker.opprettKafkaStringConsumer(
                groupId = "dittnav-consumer",
                topics = listOf(DITT_NAV_UTKAST, DITT_NAV_MICROFRONTEND)
            )
    }

    @AfterAll
    internal fun tearDown() {
        producer.close()
        dittnavBeskjedConsumer.close()
        dittnavStringConsumer.close()
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
            dittnavBeskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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
            dittnavBeskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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
            dittnavBeskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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
            dittnavBeskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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
            dittnavBeskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
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
            dittnavBeskjedConsumer.hentMelding(DITT_NAV_BESKJED) { it.getEventId() == k9Beskjed.eventId }?.value()
        validerRiktigBrukernotifikasjon(k9Beskjed, brukernotifikasjon)
    }

    @Test
    fun `Legger utkast på topic og forventer riktig dittnav utkast`() {

        val utkastId = UUID.randomUUID().toString()

        //language=JSON
        val utkast = gyldigK9Utkast(utkastId, Ytelse.PLEIEPENGER_SYKT_BARN)

        producer.leggPåTopic(utkast, K9_DITTNAV_VARSEL_UTKAST, mapper)

        val konsumertUtkast = dittnavStringConsumer.hentMelding(DITT_NAV_UTKAST) { it == utkastId }?.value()
        logger.info("Produsert utkast" + JSONObject(konsumertUtkast).toString(2))
        validerRiktigUtkast(utkast, konsumertUtkast)
    }

    @ParameterizedTest
    @EnumSource(MicrofrontendAction::class)
    @Disabled
    fun `Legger microfrontend enable event på topic og forventer riktig event publisert til dittnav`(action: MicrofrontendAction) {

        val correlationId = UUID.randomUUID().toString()

        val microfrontendEvent = gyldigK9Microfrontend(
            correlationId = correlationId,
            ident = "12345678910",
            microfrontendId = MicrofrontendId.PLEIEPENGER_INNSYN,
            action = action,
            sensitivitet = if (action == MicrofrontendAction.ENABLE) Sensitivitet.HIGH else null
        )

        producer.leggPåTopic(microfrontendEvent, K9_DITTNAV_VARSEL_MICROFRONTEND, mapper)

        val konsumertMicrofrontendEvent =
            dittnavStringConsumer.hentMelding(DITT_NAV_MICROFRONTEND) { it == correlationId }?.value()
        logger.info("Produsert microfrontend event" + JSONObject(konsumertMicrofrontendEvent).toString(2))
        validerRiktigMicrofrontendEvent(microfrontendEvent, konsumertMicrofrontendEvent)
    }
}


fun validerRiktigBrukernotifikasjon(k9Beskjed: K9Beskjed, brukernotifikasjon: BeskjedInput?) {
    assertTrue(brukernotifikasjon != null)
    assertTrue(k9Beskjed.tekst == brukernotifikasjon?.getTekst())
    k9Beskjed.link?.let { assertTrue(k9Beskjed.link == brukernotifikasjon?.getLink()) }
}

fun validerRiktigUtkast(utkast: String, konsumertUtkast: String?) {
    assertTrue(konsumertUtkast != null)
    assertTrue(JSONObject(utkast).getJSONObject("utkast").toString() == konsumertUtkast)
}

fun validerRiktigMicrofrontendEvent(k9Microfrontend: String, dittnavMicrofrontend: String?) {
    assertTrue(dittnavMicrofrontend != null)
    val publisert = JSONObject(k9Microfrontend)
    val konsumert = JSONObject(dittnavMicrofrontend)

    assertEquals(publisert.getString("ident"), konsumert.getString("ident"))
    assertEquals(publisert.getString("action").lowercase(), konsumert.getString("@action"))
    assertEquals(MicrofrontendId.valueOf(publisert.getString("microfrontendId")).id, konsumert.getString("microfrontend_id"))
    assertEquals(publisert.getString("initiatedBy"), konsumert.getString("@initiated_by"))
    assertEquals(publisert.optString("sensitivitet", null)?.lowercase(), konsumert.optString("sensitivitet", null))
}
