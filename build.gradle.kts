buildscript {
    dependencies {
        // AGP 9 compiles Kotlin itself; this picks the Kotlin version it uses
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.5.0" apply false
    id("com.android.test") version "9.5.0" apply false
    id("androidx.baselineprofile") version "1.5.0" apply false
    // Since Kotlin 2.0 the Compose compiler ships with Kotlin, versioned with it
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
