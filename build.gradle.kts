// Versions only: each module applies what it needs. The Kotlin plugin is on the classpath so AGP 9's
// built-in Kotlin compiles the library with the same compiler it was tested with.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dokka) apply false
}
