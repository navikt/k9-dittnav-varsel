package no.nav.sifinnsynapi.konsumenter

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_MICROFRONTEND
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_UTKAST
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_VARSEL
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_MICROFRONTEND
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_UTKAST
import no.nav.sifinnsynapi.utils.gyldigK9Beskjed
import no.nav.sifinnsynapi.utils.gyldigK9Microfrontend
import no.nav.sifinnsynapi.utils.gyldigK9Utkast
import no.nav.sifinnsynapi.utils.hentMelding
import no.nav.sifinnsynapi.utils.leggPåTopic
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
    topics = [K9_DITTNAV_VARSEL_BESKJED, DITT_NAV_VARSEL, K9_DITTNAV_VARSEL_UTKAST, DITT_NAV_UTKAST, K9_DITTNAV_VARSEL_MICROFRONTEND, DITT_NAV_MICROFRONTEND],
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
    lateinit var dittnavStringConsumer: Consumer<String, String> // Kafka consumer som brukes til å lese meldinger.
    lateinit var dittnavVarselConsumer: Consumer<String, String> // Kafka consumer for ny JSON-basert varsel topic

    private companion object {
        val logger = LoggerFactory.getLogger(KonsumentIntegrasjonsTest::class.java)
    }

    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer()
        dittnavStringConsumer =
            embeddedKafkaBroker.opprettKafkaStringConsumer(
                groupId = "dittnav-consumer",
                topics = listOf(DITT_NAV_UTKAST, DITT_NAV_MICROFRONTEND)
            )
        dittnavVarselConsumer =
            embeddedKafkaBroker.opprettKafkaStringConsumer(
                groupId = "dittnav-varsel-consumer",
                topics = listOf(DITT_NAV_VARSEL)
            )
    }

    @AfterAll
    internal fun tearDown() {
        producer.close()
        dittnavStringConsumer.close()
        dittnavVarselConsumer.close()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "OMSORGSDAGER_ALENEOMSORG",
        "OMSORGSPENGER_MIDLERTIDIG_ALENE",
        "OMSORGSDAGER_MELDING_OVERFØRE",
        "OMSORGSDAGER_MELDING_KORONA",
        "OMSORGSDAGER_MELDING_FORDELE",
        "ETTERSENDING_PLEIEPENGER_SYKT_BARN",
        "ETTERSENDING_PLEIEPENGER_LIVETS_SLUTTFASE",
        "ETTERSENDING_OMP",
        "ETTERSENDING_OMP_UTV_KS",
        "ETTERSENDING_OMP_UT_SNF",
        "ETTERSENDING_OMP_UT_ARBEIDSTAKER",
        "ETTERSENDING_OMP_UTV_MA",
        "ETTERSENDING_OMP_DELE_DAGER",
        "ETTERSENDING_OPPLARINGSPENGER",
        "OMSORGSPENGER_UTV_KS",
        "OMSORGSPENGER_UTVIDET_RETT",
        "OMSORGSPENGER_UT_SNF",
        "OMSORGSPENGER_UTBETALING_SNF",
        "OMSORGSPENGER_UT_ARBEIDSTAKER",
        "OMSORGSPENGER_UTBETALING_ARBEIDSTAKER",
        "PLEIEPENGER_LIVETS_SLUTTFASE",
        "PLEIEPENGER_SYKT_BARN",
        "ENDRINGSMELDING_PLEIEPENGER_SYKT_BARN",
        "OPPLARINGSPENGER",
        "UNGDOMSYTELSE"
    ])
    fun `Legger k9Beskjed på topic og forventer publisert varsel på mine sider`(ytelse: String) {
        // legg på 1 hendelse om mottatt søknad
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt din søknad for $ytelse.",
            link = "https://www.nav.no",
            ytelse = ytelse
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        // Verifiser at JSON varsel blir publisert riktig uten link.
        val jsonVarsel = dittnavVarselConsumer.hentMelding(DITT_NAV_VARSEL) { it == k9Beskjed.eventId }?.value()
        validerRiktigJsonVarsel(k9Beskjed, jsonVarsel)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "OMSORGSDAGER_ALENEOMSORG",
        "OMSORGSPENGER_MIDLERTIDIG_ALENE",
        "OMSORGSDAGER_MELDING_OVERFØRE",
        "OMSORGSDAGER_MELDING_KORONA",
        "OMSORGSDAGER_MELDING_FORDELE",
        "ETTERSENDING_PLEIEPENGER_SYKT_BARN",
        "ETTERSENDING_PLEIEPENGER_LIVETS_SLUTTFASE",
        "ETTERSENDING_OMP",
        "ETTERSENDING_OMP_UTV_KS",
        "ETTERSENDING_OMP_UT_SNF",
        "ETTERSENDING_OMP_UT_ARBEIDSTAKER",
        "ETTERSENDING_OMP_UTV_MA",
        "ETTERSENDING_OMP_DELE_DAGER",
        "ETTERSENDING_OPPLARINGSPENGER",
        "OMSORGSPENGER_UTV_KS",
        "OMSORGSPENGER_UTVIDET_RETT",
        "OMSORGSPENGER_UT_SNF",
        "OMSORGSPENGER_UTBETALING_SNF",
        "OMSORGSPENGER_UT_ARBEIDSTAKER",
        "OMSORGSPENGER_UTBETALING_ARBEIDSTAKER",
        "PLEIEPENGER_LIVETS_SLUTTFASE",
        "PLEIEPENGER_SYKT_BARN",
        "ENDRINGSMELDING_PLEIEPENGER_SYKT_BARN",
        "OPPLARINGSPENGER",
        "UNGDOMSYTELSE"
    ])
    fun `Legger K9Beskjed uten link på topic og forventer riktig JSON varsel`(ytelse: String) {
        // Test JSON varsel uten link
        val k9Beskjed = gyldigK9Beskjed(
            tekst = "Vi har mottatt din søknad for $ytelse.",
            link = null,
            ytelse = ytelse
        )

        producer.leggPåTopic(k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, mapper)

        // Verifiser at JSON varsel blir publisert riktig uten link.
        val jsonVarsel = dittnavVarselConsumer.hentMelding(DITT_NAV_VARSEL) { it == k9Beskjed.eventId }?.value()
        validerRiktigJsonVarsel(k9Beskjed, jsonVarsel)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "OMSORGSDAGER_ALENEOMSORG",
        "OMSORGSPENGER_MIDLERTIDIG_ALENE",
        "OMSORGSDAGER_MELDING_OVERFØRE",
        "OMSORGSDAGER_MELDING_KORONA",
        "OMSORGSDAGER_MELDING_FORDELE",
        "ETTERSENDING_PLEIEPENGER_SYKT_BARN",
        "ETTERSENDING_PLEIEPENGER_LIVETS_SLUTTFASE",
        "ETTERSENDING_OMP",
        "ETTERSENDING_OMP_UTV_KS",
        "ETTERSENDING_OMP_UT_SNF",
        "ETTERSENDING_OMP_UT_ARBEIDSTAKER",
        "ETTERSENDING_OMP_UTV_MA",
        "ETTERSENDING_OMP_DELE_DAGER",
        "ETTERSENDING_OPPLARINGSPENGER",
        "OMSORGSPENGER_UTV_KS",
        "OMSORGSPENGER_UTVIDET_RETT",
        "OMSORGSPENGER_UT_SNF",
        "OMSORGSPENGER_UTBETALING_SNF",
        "OMSORGSPENGER_UT_ARBEIDSTAKER",
        "OMSORGSPENGER_UTBETALING_ARBEIDSTAKER",
        "PLEIEPENGER_LIVETS_SLUTTFASE",
        "PLEIEPENGER_SYKT_BARN",
        "ENDRINGSMELDING_PLEIEPENGER_SYKT_BARN",
        "OPPLARINGSPENGER",
        "UNGDOMSYTELSE"
    ])
    fun `Legger utkast på topic og forventer riktig dittnav utkast`(ytelse: String) {

        val utkastId = UUID.randomUUID().toString()

        //language=JSON
        val utkast = gyldigK9Utkast(utkastId, ytelse)

        producer.leggPåTopic(utkast, K9_DITTNAV_VARSEL_UTKAST, mapper)

        val konsumertUtkast = dittnavStringConsumer.hentMelding(DITT_NAV_UTKAST) { it == utkastId }?.value()
        logger.info("Produsert utkast" + JSONObject(konsumertUtkast).toString(2))
        validerRiktigUtkast(utkast, konsumertUtkast)
    }

    @ParameterizedTest
    @EnumSource(MicrofrontendAction::class)
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

fun validerRiktigJsonVarsel(k9Beskjed: K9Beskjed, jsonVarsel: String?) {
    assertTrue(jsonVarsel != null)
    val konsumertJson = JSONObject(jsonVarsel)

    // Valider at ny JSON varsel format er i henhold til dokumentasjonen.
    assertEquals("opprett", konsumertJson.getString("@event_name"))
    assertEquals("beskjed", konsumertJson.getString("type"))
    assertEquals(k9Beskjed.eventId, konsumertJson.getString("varselId"))
    assertEquals("high", konsumertJson.getString("sensitivitet"))
    assertEquals(k9Beskjed.søkerFødselsnummer, konsumertJson.getString("ident"))

    // Valider tekster
    val tekster = konsumertJson.getJSONArray("tekster")
    assertTrue(tekster.length() == 1)
    val tekst = tekster.getJSONObject(0)
    assertEquals("nb", tekst.getString("spraakkode"))
    assertEquals(k9Beskjed.tekst, tekst.getString("tekst"))
    assertTrue(tekst.getBoolean("default"))

    // Valider valgfri link
    if (k9Beskjed.link != null) {
        assertEquals(k9Beskjed.link, konsumertJson.getString("link"))
    } else {
        assertTrue(!konsumertJson.has("link") || konsumertJson.isNull("link"))
    }

    // Valider aktivFremTil eksisterer
    assertTrue(konsumertJson.has("aktivFremTil"))

    // Valider produsent
    val produsent = konsumertJson.getJSONObject("produsent")
    assertEquals("prod-gcp", produsent.getString("cluster"))
    assertEquals("dusseldorf", produsent.getString("namespace"))
    assertEquals("k9-dittnav-varsel", produsent.getString("appnavn"))
}
