rootProject.name = "k9-dittnav-varsel"

plugins {
    id("com.gradle.develocity") version("3.19")
}

develocity {
    if (System.getenv("CI") != null) {
        buildScan {
            publishAlways()
            termsOfUseUrl = "https://gradle.com/terms-of-service"
            termsOfUseAgree = "yes"
        }
    }
}