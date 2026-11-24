import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// The library a third-party app on a Facilipay terminal uses to call Facilipay
// (io.facilipay:terminal-sdk). Third parties build it into their own apps, so it has no dependency
// but androidx.activity, a low minSdk, Java 11 bytecode and Kotlin 2.0 metadata: it works in older
// projects too. The guide is in docs/, the shared protocol in protocol/.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    `maven-publish`
}

group = "io.facilipay"
version = providers.gradleProperty("terminalSdkVersion").getOrElse("1.0.0-SNAPSHOT")

android {
    namespace = "io.facilipay.terminal"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // AGP otherwise requires integrators to compile against this library's compileSdk. The
        // library uses no API newer than its minSdk.
        aarMetadata {
            minCompileSdk = 21
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        // androidx.activity stays at the oldest version with ActivityResultContract on purpose (see
        // dependencies), so the "newer version available" check doesn't apply.
        disable += "GradleDependency"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

kotlin {
    explicitApi()
    // The kotlin-stdlib the POM asks for. A newer one cannot be read by an older Kotlin compiler in
    // the integrator's project.
    coreLibrariesVersion = "2.0.21"
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        // Integrators may still be on an older Kotlin compiler; it reads metadata one version newer.
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    // Only ActivityResultContract, which 1.2 introduced. The oldest version that has it, so an
    // integrator's newer one wins and older projects (lower compileSdk) still resolve it.
    api(libs.androidx.activity.terminal.sdk)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit4)

    // Device checks of the Android glue (intents, availability) on a terminal.
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(kotlin("test-junit"))
}

// GiftProtocolContractTest reads the shared protocol file and this library's manifest. Declared as
// inputs, so editing only one of them re-runs the test.
tasks.withType<Test>().configureEach {
    jvmArgumentProviders += ProtocolTestFiles(
        protocol = rootProject.layout.projectDirectory.file("protocol/gift-intent-protocol.properties"),
        manifest = layout.projectDirectory.file("src/main/AndroidManifest.xml"),
    )
}

class ProtocolTestFiles(
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val protocol: RegularFile,
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val manifest: RegularFile,
) : CommandLineArgumentProvider {
    override fun asArguments() = listOf(
        "-Dfacilipay.protocolFile=${protocol.asFile.absolutePath}",
        "-Dfacilipay.libraryManifest=${manifest.asFile.absolutePath}",
    )
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "io.facilipay"
            artifactId = "terminal-sdk"
            afterEvaluate { from(components["release"]) }
            pom {
                name.set("Facilipay Terminal SDK")
                description.set(
                    "Lets an Android app on a Facilipay terminal start gift card top-ups, " +
                        "redemptions and balance checks in Facilipay and read the result.",
                )
                url.set("https://developers.facilipay.io/terminal/")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit")
                    }
                }
                organization {
                    name.set("Facilipay")
                    url.set("https://facilipay.io")
                }
                developers {
                    developer {
                        id.set("facilipay")
                        name.set("Facilipay")
                        url.set("https://facilipay.io")
                    }
                }
                scm {
                    url.set("https://github.com/Facilipay/facilipay-terminal-android")
                    connection.set("scm:git:https://github.com/Facilipay/facilipay-terminal-android.git")
                }
            }
        }
    }
    repositories {
        // A plain directory laid out as a Maven repository. CI uploads it to
        // developers.facilipay.io/maven (or the staging site); locally it feeds the sample app.
        maven {
            name = "terminalSdk"
            url = uri(
                providers.gradleProperty("terminalSdkRepo")
                    .getOrElse(layout.buildDirectory.dir("maven-repo").get().asFile.path),
            )
        }
    }
}

// The API reference on the guide site (developers.facilipay.io/terminal/api/): Dokka's HTML shows
// Kotlin signatures with their nullability, which the Javadoc format loses.
dokka {
    moduleName.set("terminal-sdk")
    dokkaSourceSets.configureEach {
        includes.from("dokka-module.md")
        skipEmptyPackages.set(true)
    }
}
