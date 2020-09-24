package no.nav.sifinnsynapi.innsyn

import assertk.assertThat
import assertk.assertions.isNotEmpty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.config.Topics.DITT_NAV_BESKJED
import no.nav.sifinnsynapi.config.Topics.INNSYN_MOTTATT
import no.nav.sifinnsynapi.utils.leggPåTopic
import no.nav.sifinnsynapi.utils.lesMelding
import no.nav.sifinnsynapi.utils.opprettDittnavConsumer
import no.nav.sifinnsynapi.utils.opprettKafkaProducer
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
import java.util.*
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
        topics = [INNSYN_MOTTATT, DITT_NAV_BESKJED],
        bootstrapServersProperty = "spring.kafka.bootstrap-servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class InnsynHendelseKonsumentIntegrasjonsTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om mottat søknad i innsyn.
    lateinit var dittNavConsumer: Consumer<Nokkel, Beskjed> // Kafka consumer som brukes til å lese kafka meldinger.

    companion object {
        private val log: Logger = LoggerFactory.getLogger(InnsynHendelseKonsumentIntegrasjonsTest::class.java)
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
    fun `gitt konsumert innsynsmelding, forvent publisert dittnav beskjed`() {

        // legg på 1 hendelse om mottatt søknad om pleiepenger sykt barn...
        val melding = InnsynMelding(
                metadata = Metadata(
                    version = 1,
                    correlationId = UUID.randomUUID().toString(),
                    requestId = UUID.randomUUID().toString()
                ),
                grupperingsId = "pleiepenger-sykt-barn",
                eventId = UUID.randomUUID().toString(),
                søkerFødselsnummer = "12345678910",
                tekst = "Vi har mottatt din søknad om pleiepenger - sykt barn. Klikk undr for mer info.",
                link = "https://www.nav.no",
                dagerSynlig = 7
        )
        producer.leggPåTopic(melding, INNSYN_MOTTATT, mapper)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gitt forventet resultat.
        await.atMost(60, TimeUnit.SECONDS).untilAsserted {

            val lesMelding = dittNavConsumer.lesMelding(melding.eventId)
            log.info("----> dittnav melding: {}", lesMelding)
            assertThat(lesMelding).isNotEmpty()
        }
    }
}

