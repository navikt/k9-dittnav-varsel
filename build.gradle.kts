import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "7.1.0.6387"
    jacoco
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val tmsVarselVersjon by extra("2.1.1")
val logstashLogbackEncoderVersion by extra("7.4")
val retryVersion by extra("2.0.5")
val jsonVersion by extra("20240303")
val awaitilityKotlinVersion by extra("4.2.1")
val assertkJvmVersion by extra("0.28.0")
val springMockkVersion by extra("4.0.2")
val mockkVersion by extra("1.13.10")

repositories {
    mavenCentral()

    maven {
        name = "github-package-registry-navikt"
        url = uri("https://maven.pkg.github.com/navikt/maven-release")
    }

    maven {
        name = "github-package-registry-mirror-navikt"
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }

    maven {
        name = "confluent"
        url = uri("https://packages.confluent.io/maven/")
    }

    maven {
        url = uri("https://jitpack.io")
    }
}
dependencies {
    implementation("org.yaml:snakeyaml:2.5") {
        because("https://github.com/navikt/k9-dittnav-varsel/security/dependabot/2")
    }

    // NAV
    implementation("com.github.navikt:tms-mikrofrontend-selector:20231005112556-1c554d9")
    implementation("no.nav.tms.varsel:kotlin-builder:$tmsVarselVersjon")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.retry:spring-retry:$retryVersion")
    implementation("org.springframework:spring-aspects")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    //Kafka
    implementation("org.springframework.kafka:spring-kafka")
    constraints {
        implementation("org.scala-lang:scala-library") {
            because("org.apache.kafka:kafka_2.13:3.3.2 -> https://www.cve.org/CVERecord?id=CVE-2022-36944")
            version {
                require("2.13.9")
            }
        }
    }
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Diverse
    implementation("org.json:json:$jsonVersion")
    implementation("com.github.ben-manes.caffeine:caffeine")
    // Flere sikkerhetsfil. Transitiv fra avro og kafka-connect-avro-converter, så kan fjernes når de er oppdatert. 
    implementation("org.apache.commons:commons-compress:1.28.0")


    testImplementation("org.awaitility:awaitility-kotlin:$awaitilityKotlinVersion")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkJvmVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport) // report is always generated after tests run
    }

    jacocoTestReport {
        dependsOn(test) // tests are required to run before generating the report
        reports {
            xml.required.set(true)
            csv.required.set(false)
        }
    }

    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    getByName<Jar>("jar") {
        enabled = false
    }

    withType<Wrapper> {
        gradleVersion = "8.2.1"
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_k9-dittnav-varsel")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.sourceEncoding", "UTF-8")
    }
}
