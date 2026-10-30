package com.longweekend.optimizer

import android.os.Bundle
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.longweekend.optimizer.repository.SettingsRepository
import com.longweekend.optimizer.ui.navigation.AppNavigation
import com.longweekend.optimizer.ui.theme.LongWeekendTheme
import com.longweekend.optimizer.ui.theme.SurfaceBg
import com.longweekend.optimizer.ui.theme.applyNightMode
import com.longweekend.optimizer.viewmodel.MainViewModel

// AppCompatActivity applies the in-app language and appearance chosen in the profile
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, so the saved appearance applies without an extra recreate
        SettingsRepository.getInstance(this).themeMode.value.applyNightMode()
        super.onCreate(savedInstanceState)
        // Targeting API 35+ makes edge-to-edge mandatory; opt in explicitly so all versions match.
        // System-bar icons follow the app's appearance (set above), not only the phone's.
        val dark = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val barStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        setContent {
            LongWeekendTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceBg
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
