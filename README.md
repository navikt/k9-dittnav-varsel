# K9 Dittnav Varsel

![CI / CD](https://github.com/navikt/https://github.com/navikt/k9-dittnav-varsel/actions/workflows/CI%20/%20CD/badge.svg)
![NAIS Alerts](https://github.com/navikt/https://github.com/navikt/k9-dittnav-varsel/actions/workflows/Alerts/badge.svg)

# Innholdsoversikt
* [1. Kontekst](#1-kontekst)
* [2. Funksjonelle Krav](#2-funksjonelle-krav)
* [3. Begrensninger](#3-begrensninger)
* [4. Programvarearkitektur](#5-programvarearkitektur)
* [5. Kode](#6-kode)
* [6. Data](#7-data)
* [7. Infrastrukturarkitektur](#8-infrastrukturarkitektur)
* [8. Distribusjon av tjenesten (deployment)](#9-distribusjon-av-tjenesten-deployment)
* [9. Utviklingsmiljø](#10-utviklingsmilj)
* [10. Drift og støtte](#11-drift-og-sttte)

# 1. Kontekst
Kafka konsumer og producer som fungerer som et mellomledd mellom sif-brukerdialog og andre varslingstjenester som dittnav.

# 2. Funksjonelle Krav
Denne tjenesten understøtter behovet for varsling til bruker.
Et varsel kan være av følgende formater:
* Dittnav beskjed
* SMS
* Epost

K9-dittnav-varsel eksponerer en topice hvor det kan legges en K9Beskjed på som mappes om til dittnav-beskjed og sendes videre. 

I Aiven heter topicen **_dusseldorf.privat-k9-dittnav-varsel-beskjed_**.

Det er ønskelig at tjenestene som egentlig ikke har noe med innsyn migrerer over til å legge
K9Beskjed direkte på topicen, og ikke gjennom sif-innsyn-api.

# 3. Begrensninger
Denne tjenesten begrenses sykdom-i-familien og er ment å brukes som et felles komponent til varsling for andre tjenester i teamet.

# 4. Programvarearkitektur

# 5. Kode
K9Beskjed:
```
data class K9Beskjed(
    val metadata: Metadata,
    val grupperingsId: String,
    val tekst: String,
    val link: String? = null,
    val dagerSynlig: Long,
    val søkerFødselsnummer: String,
    val eventId: String,
    val ytelse: Ytelse? = null
)

enum class Ytelse{
    OMSORGSDAGER_ALENEOMSORG,
    OMSORGSPENGER_MIDLERTIDIG_ALENE,
    OMSORGSDAGER_MELDING_OVERFØRE,
    OMSORGSDAGER_MELDING_KORONA,
    OMSORGSDAGER_MELDING_FORDELE,
    ETTERSENDING_PLEIEPENGER_SYKT_BARN,
    ETTERSENDING_OMP_UTV_KS, // Ettersending - Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    ETTERSENDING_OMP_UT_SNF, // Ettersending - Omsorgspenger utbetaling SNF ytelse.
    ETTERSENDING_OMP_UT_ARBEIDSTAKER, // Ettersending - Omsorgspenger utbetaling arbeidstaker ytelse.
    ETTERSENDING_OMP_UTV_MA, // Ettersending - Omsorgspenger utvidet rett - midlertidig alene
    ETTERSENDING_OMP_DELE_DAGER, // Ettersending - Melding om deling av omsorgsdager,
    OMSORGSPENGER_UTV_KS, // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
    OMSORGSPENGER_UT_SNF, // Omsorgspenger utbetaling snf
    OMSORGSPENGER_UT_ARBEIDSTAKER, // Omsorgspenger utbetaling arbeidstaker
    PLEIEPENGER_LIVETS_SLUTTFASE
}
```


# 6. Data

# 7. Infrastrukturarkitektur



# 8. Distribusjon av tjenesten (deployment)
Distribusjon av tjenesten er gjort med bruk av Github Actions.
[K9 Dittnav Varsel CI / CD](https://github.com/navikt/k9-dittnav-varsel/actions)

Push/merge til master branche vil teste, bygge og deploye til produksjonsmiljø og testmiljø.

# 9. Utviklingsmiljø
## Forutsetninger
* docker
* docker-compose
* Java 11
* Kubectl

## Bygge Prosjekt
For å bygge kode, kjør:

```shell script
./gradlew clean build
```

## Kjøre Prosjekt
For å kjøre kode, kjør:

```shell script
./gradlew clean build && docker build --tag k9-dittnav-varsel-local . && docker-compose up --build
```

Eller for å hoppe over tester under bygging:
```shell script
./gradlew clean build -x test && docker build --tag k9-dittnav-varsel-local . && docker-compose up --build
```

### Produsere kafka meldinger
For produsere kafka meldinger, må man først exec inn på kafka kontaineren ved å bruker docker dashbord, eller ved å kjøre følgende kommando:
```shell script
docker exec -it <container-id til kafka> /bin/sh; exit
```

Deretter, kjøre følgende kommando for å koble til kafka instansen:
```shell script
kafka-console-producer --broker-list localhost:9092 --topic privat-sif-innsyn-mottak --producer.config=$CLASSPATH/producer.properties
```

# 10. Drift og støtte
## Logging
Loggene til tjenesten kan leses på to måter:

### Kibana
For [dev-gcp: https://logs.adeo.no/goto/7db198143c27f93228b17f3b07f16e39](https://logs.adeo.no/goto/7db198143c27f93228b17f3b07f16e39)
For [prod-gcp: https://logs.adeo.no/goto/e796ec96af7bb1032a11d388e6849451](https://logs.adeo.no/goto/e796ec96af7bb1032a11d388e6849451)

### Kubectl
For dev-gcp:
```shell script
kubectl config use-context dev-gcp
kubectl get pods -n dusseldorf | grep k9-dittnav-varsel
kubectl logs -f k9-dittnav-varsel-<POD-ID> --namespace dusseldorf -c k9-dittnav-varsel
```

For prod-gcp:
```shell script
kubectl config use-context prod-gcp
kubectl get pods -n dusseldorf | grep k9-dittnav-varsel
kubectl logs -f k9-dittnav-varsel-<POD-ID> --namespace dusseldorf -c k9-dittnav-varsel
```

## Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [nais/alerterator.yml](nais/alerterator.yml).

## Metrics

## Henvendelser
Spørsmål koden eller prosjekttet kan rettes til team dusseldorf på:
* [\#sif-brukerdialog](https://nav-it.slack.com/archives/CQ7QKSHJR)


