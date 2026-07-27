plugins {
    // Kotlin support is built into AGP 9+; org.jetbrains.kotlin.android is no longer applied.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
