plugins {
  id("org.jetbrains.kotlin.jvm") version "2.0.21"
  id("io.ktor.plugin") version "3.0.1"
  id("application")
  `jvm-test-suite`
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.kotlinx.cli)
}

val mainClassName = "ApplicationKt"

application {
  mainClass.set(mainClassName)
}

ktor {
  fatJar {
    archiveFileName.set("gradle-dependency-diff.jar")
  }
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
      dependencies {
        implementation(libs.kotest.runner.junit5)
        implementation(libs.kotest.assertions.core)
      }
    }
  }
}
