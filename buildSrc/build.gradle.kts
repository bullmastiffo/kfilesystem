plugins {
    `kotlin-dsl`
    id("org.jetbrains.dokka") version "1.4.10.2"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("gradle-plugin", "1.4.10"))
}