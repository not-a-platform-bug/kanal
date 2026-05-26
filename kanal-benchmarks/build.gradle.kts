plugins {
    kotlin("jvm")
    application
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
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
}

dependencies {
    implementation(project(":kanal-core"))
    implementation(project(":kanal-runtime"))
}

application {
    mainClass.set("io.github.kimseungjin.kanal.benchmarks.KanalBenchmarkKt")
}
