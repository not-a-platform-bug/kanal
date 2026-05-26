plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(24)
}

kotlin {
    compilerOptions {
        // Kotlin does not expose a JVM 25 bytecode target yet, so we keep the
        // toolchain on 25 while emitting the newest supported target.
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
}

dependencies {
    api(project(":kanal-core"))
    api(project(":kanal-runtime"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("tools.jackson.module:jackson-module-kotlin")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

tasks.withType<Test> {
    useJUnitPlatform()
}
