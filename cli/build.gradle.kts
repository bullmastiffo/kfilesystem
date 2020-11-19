
plugins {
    id("vifs.cli")
}

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass.set("com.mvg.app.AppKt")

}
val run: JavaExec by tasks
run.standardInput = System.`in`

