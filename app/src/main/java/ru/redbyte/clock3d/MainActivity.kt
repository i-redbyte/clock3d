package ru.redbyte.clock3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import ru.redbyte.clock3d.gl.ClockMode
import ru.redbyte.clock3d.ui.ClockScreen

private val AppBarColor = Color(0xFF040806)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMode = ClockMode.fromLaunchExtra(intent.getStringExtra(EXTRA_CLOCK_MODE))
        val bar = AppBarColor.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(bar),
            navigationBarStyle = SystemBarStyle.dark(bar),
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(),
            ) {
                ClockScreen(initialMode = initialMode)
            }
        }
    }

    companion object {
        /** Launch extra for demos: `classic`, `dali`, `spheres`, `ice`. */
        const val EXTRA_CLOCK_MODE = "CLOCK_MODE"
    }
}
