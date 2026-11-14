plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 intègre Kotlin : kotlin.android n'est jamais appliqué, il sert seulement à fixer la version de Kotlin
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
