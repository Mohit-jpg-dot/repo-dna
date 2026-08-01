plugins {
    java
    application
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.repodna"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

application {
    mainClass.set("com.repodna.cli.RepoDnaCli")
}

repositories {
    mavenCentral()
}

dependencies {
    // Picocli
    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")

    // Jackson Configuration Parser
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    // Logging: SLF4J + Logback
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // Testing: JUnit 5 + AssertJ
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.repodna.cli.RepoDnaCli"
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Aproject=com.repodna/repo-dna")
}
