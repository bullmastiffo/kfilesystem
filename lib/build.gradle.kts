import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    id("vifs.lib")
    id("org.jetbrains.dokka") version "1.4.10.2"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/bullmastiffo/kfilesystem/tree/master/" +
                        "lib/src/main/kotlin"
                ))
                remoteLineSuffix.set("#L")
            }
        }
    }
}