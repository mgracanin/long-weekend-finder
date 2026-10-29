package com.longweekend.optimizer

import android.os.Bundle
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.longweekend.optimizer.ui.navigation.AppNavigation
import com.longweekend.optimizer.ui.theme.LongWeekendTheme
import com.longweekend.optimizer.ui.theme.SurfaceBg
import com.longweekend.optimizer.viewmodel.MainViewModel

// AppCompatActivity applies the in-app language chosen in the profile
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Targeting API 35+ makes edge-to-edge mandatory; opt in explicitly so all versions match.
        // The app is always light, so keep dark system-bar icons even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            LongWeekendTheme {
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
