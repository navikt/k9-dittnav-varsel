import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}
val springfoxVersion by extra("2.9.2")
val confluentVersion by extra("5.5.0")
val avroVersion by extra("1.9.2")
val brukernotifikasjonVersion by extra("1.2021.01.18-11.12-b9c8c40b98d1")
val logstashLogbackEncoderVersion by extra("6.3")
val tokenValidationVersion by extra("1.1.5")
val retryVersion by extra("1.3.0")
val zalandoVersion by extra("0.25.2")
val jsonVersion by extra("20210307")
val awaitilityKotlinVersion by extra("4.1.0")
val assertkJvmVersion by extra("0.24")
val springMockkVersion by extra("3.0.1")
val mockkVersion by extra("1.12.0")

ext["log4j2.version"] = "2.16.0" // TODO: 13/12/2021 kan fjernes når spring boot oppgraderes til  v2.5.8 eller v2.6.2

repositories {
    mavenCentral()

    maven {
        name = "github-package-registry-navikt"
        url = uri("https://maven.pkg.github.com/navikt/maven-releas")
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

    // NAV
    implementation("com.github.navikt:brukernotifikasjon-schemas:$brukernotifikasjonVersion")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.retry:spring-retry:$retryVersion")
    implementation("org.springframework:spring-aspects")
    runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit")
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")

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
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-connect-avro-converter:$confluentVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Diverse
    implementation("org.json:json:$jsonVersion")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.zalando:problem-spring-web-starter:$zalandoVersion")

    testImplementation("org.awaitility:awaitility-kotlin:$awaitilityKotlinVersion")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkJvmVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.getByName<Jar>("jar") {
    enabled = false
}
