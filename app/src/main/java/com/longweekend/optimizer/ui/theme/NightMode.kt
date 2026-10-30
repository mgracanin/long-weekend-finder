package com.longweekend.optimizer.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.longweekend.optimizer.repository.ThemeMode

/**
 * Applies the appearance app-wide through AppCompat, so the window background, system bars and
 * Compose's isSystemInDarkTheme() all agree; changing it recreates the activity like a phone switch.
 */
fun ThemeMode.applyNightMode() {
    AppCompatDelegate.setDefaultNightMode(
        when (this) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
    )
}
