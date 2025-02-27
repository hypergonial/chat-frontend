package com.hypergonial.chat

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arkivanov.decompose.defaultComponentContext
import com.hypergonial.chat.model.AndroidSettings
import com.hypergonial.chat.model.settings
import com.hypergonial.chat.view.components.DefaultRootComponent
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import io.github.vinceglb.filekit.core.FileKit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        enableEdgeToEdge()
        initializeStorage()
        val root = DefaultRootComponent(defaultComponentContext())

        ContextHelper.retrieveAppContext = { this.applicationContext }

        setContent { AppTheme { App(root) } }
    }

    override fun onPause() {
        super.onPause()
        ContextHelper.retrieveAppContext = { null }
    }

    override fun onResume() {
        super.onResume()
        ContextHelper.retrieveAppContext = { this.applicationContext }
        initializeStorage()
    }

    override fun onDestroy() {
        super.onDestroy()
        ContextHelper.retrieveAppContext = { null }
    }

    private fun initializeStorage() {
        if (settings !is AndroidSettings) {
            return
        }

        settings.initialize(getSharedPreferences("settings", MODE_PRIVATE))
    }
}

/// Adaptive theming depending on system theme.
@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Material You is only supported on Android 12+
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme =
        when {
            supportsDynamicColor && useDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
            supportsDynamicColor && !useDarkTheme -> dynamicLightColorScheme(LocalContext.current)
            else ->
                dynamicColorScheme(
                    seedColor = Color(104, 165, 39),
                    isDark = useDarkTheme,
                    isAmoled = false,
                    style = PaletteStyle.TonalSpot,
                )
        }

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
