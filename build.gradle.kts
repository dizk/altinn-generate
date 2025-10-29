plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.openapi.generator") version "7.16.0"
}

group = "com.github.dizk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(24)
}

val openApiOutFolder = layout.buildDirectory.dir("generated/openapi").get().asFile

openApiGenerate {
    generatorName.set("kotlin")
    // This is the OpenAPI spec
    inputSpec.set("$rootDir/src/main/resources/schema/spec.json")
    outputDir.set("$openApiOutFolder")
    modelPackage.set("com.github.dizk.models")
    additionalProperties.set(
        mapOf(
            "serializationLibrary" to "kotlinx_serialization"
        )
    )
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "models" to "",
            "modelDocs" to "",
            "modelTests" to "",
        )
    )
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("$openApiOutFolder/src/main/kotlin")
    }
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
}