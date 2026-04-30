import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jline:jline:4.0.12:jdk11")
    implementation("org.jline:jline-terminal-jni:4.0.12")
    implementation("com.esotericsoftware:kryo:5.6.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

val mainJavaSources = listOf(
    "apps/client/src/main/java",
    "apps/server/src/main/java",
    "apps/tools/integration/src/main/java",
    "apps/tools/local-harness/src/main/java",
    "apps/tools/terminal-client/src/main/java",
    "apps/tools/terminal-server/src/main/java",
    "libs/foundation/src/main/java",
    "libs/simulation-core/src/main/java"
)

sourceSets {
    named("main") {
        java.setSrcDirs(mainJavaSources)
        resources.setSrcDirs(emptyList<String>())
    }

    named("test") {
        java.setSrcDirs(listOf("libs/simulation-core/src/test/java"))
        resources.setSrcDirs(emptyList<String>())
    }
}

application {
    mainClass.set("com.factoryonline.server.Main")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(11)
}

fun configureRuntimeTask(task: JavaExec, runtimeMainClass: String) {
    task.group = ApplicationPlugin.APPLICATION_GROUP
    task.classpath = sourceSets["main"].runtimeClasspath
    task.mainClass.set(runtimeMainClass)
    task.standardInput = System.`in`
    task.standardOutput = System.out
    task.errorOutput = System.err
    task.workingDir = projectDir
}

val runServer = tasks.register("runServer", JavaExec::class.java) {
    description = "Runs the split server application."
    configureRuntimeTask(this, "com.factoryonline.server.Main")
}

tasks.named("run", JavaExec::class.java) {
    description = "Runs the split server application."
    configureRuntimeTask(this, "com.factoryonline.server.Main")
}

tasks.register("runClient", JavaExec::class.java) {
    description = "Runs the split client application."
    configureRuntimeTask(this, "com.factoryonline.client.Main")
}

tasks.register("tickerSchedulerTest", JavaExec::class.java) {
    group = "verification"
    description = "Runs the plain Java ticker/scheduler regression tests."
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.factoryonline.simulation.tick.TickerSchedulerTest")
}

tasks.named("check") {
    dependsOn("tickerSchedulerTest")
}
