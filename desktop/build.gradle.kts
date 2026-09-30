plugins {
  kotlin("jvm") version "2.4.20"
}

group = "com.planetofoses989.kwideo"
version = "0.1.0"

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}

kotlin {
  jvmToolchain(17)
}
