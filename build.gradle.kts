plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "io.github.kimseungjin.kanal"
version = "0.1.0-SNAPSHOT"

subprojects {
    repositories {
        mavenCentral()
    }
}
