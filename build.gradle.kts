plugins {
	java
	// Dependencies list and diff automation in command line and CI/CD
	id("org.cyclonedx.bom") version "2.3.1"
}

group = "com.example.template-java-gradle-spring"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {

	// TODO upgrade to 5.3
	implementation("com.github.jsqlparser:jsqlparser:4.9")

	implementation("ch.qos.logback:logback-classic:1.5.18")
	implementation("com.google.guava:guava:33.4.8-jre")
	implementation("org.apache.commons:commons-lang3:3.17.0")
	// Runtime validation alternative...
	implementation("org.assertj:assertj-core:3.27.3")

	// Tests
	testImplementation("org.junit.jupiter:junit-jupiter:5.13.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

tasks.withType<JavaCompile> {
	options.isDeprecation = true
	// Enables http unit tests
	options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed", "standardOut", "standardError")
	}
}

// Disable the default cyclonedxBom task: https://github.com/CycloneDX/cyclonedx-gradle-plugin/issues/596
tasks.named("cyclonedxBom") {
	enabled = false
}

// Example: gradle sbom; vk-sbom-diff sbom-1.json sbom.json
tasks.register("sbom", org.cyclonedx.gradle.CycloneDxTask::class) {
	setIncludeConfigs(listOf("runtimeClasspath"))
	setProjectType("application")
	setSchemaVersion("1.6")
	setDestination(project.file("."))
	setOutputName("sbom")
	setOutputFormat("json")
	setIncludeBomSerialNumber(false)
	setIncludeLicenseText(false)
}
