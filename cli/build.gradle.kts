
plugins {
    id("vifs.cli")
}

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass.set("com.mvg.app.AppKt")
}
