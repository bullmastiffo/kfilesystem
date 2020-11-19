plugins {
    kotlin("jvm")
    `kotlin-kapt`
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/arrow-kt/arrow-kt/")
    }
}

val arrow_version = "0.11.0"
dependencies {
    implementation(kotlin("stdlib", "1.4.10"))
    implementation("io.arrow-kt:arrow-core:$arrow_version")
    implementation("io.arrow-kt:arrow-syntax:$arrow_version")
    kapt("io.arrow-kt:arrow-meta:$arrow_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.mockk:mockk:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}