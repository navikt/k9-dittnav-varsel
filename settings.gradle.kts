rootProject.name = "k9-dittnav-varsel"

plugins {
    id("com.gradle.develocity") version("4.3.2")
}

develocity {
    buildScan {
        publishing.onlyIf { System.getenv("CI") != null }
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}