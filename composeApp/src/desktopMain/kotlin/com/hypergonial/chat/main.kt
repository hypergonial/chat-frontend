package com.hypergonial.chat

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.hypergonial.chat.view.colors.colorProvider
import com.hypergonial.chat.view.components.DefaultRootComponent
import com.hypergonial.chat.view.composables.Material3ContextMenuRepresentation
import com.hypergonial.chat.view.globalKeyEventFlow
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.rememberDynamicMaterialThemeState
import java.awt.Dimension
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import java.io.File

private const val SAVED_STATE_FILE_NAME = "state.dat"

/// Adaptive theming depending on system theme.
@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {

    val seedColor = colorProvider.getAccentColorOfOS() ?: Color(117, 156, 223)

    val dynamicThemeState = rememberDynamicMaterialThemeState(
        seedColor = seedColor,
        isDark = useDarkTheme,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
    )

    CompositionLocalProvider(LocalUsingDarkTheme provides useDarkTheme) {
        CompositionLocalProvider(LocalContextMenuRepresentation provides Material3ContextMenuRepresentation()) {
            DynamicMaterialTheme(state = dynamicThemeState, animate = true, content = content)
        }
    }
}

fun main() {
    Logger.setLogWriters(Slf4jLogWriter())
    val lifecycle = LifecycleRegistry()
    // Deserialize state from state file
    val stateKeeper = StateKeeperDispatcher((File(SAVED_STATE_FILE_NAME).readToSerializableContainer()))

    val root = runOnUiThread { DefaultRootComponent(ctx = DefaultComponentContext(lifecycle, stateKeeper)) }

    application {
        val windowState = rememberWindowState(width = 1024.dp, height = 768.dp)
        LifecycleController(lifecycle, windowState)

        Window(
            onCloseRequest = {
                stateKeeper.save().writeToFile(File(SAVED_STATE_FILE_NAME))
                exitApplication()
            },
            title = "Chat",
            state = windowState,
            onKeyEvent = { event ->
                globalKeyEventFlow.send(event)
                false
            },
        ) {
            // TODO: Handle window focus events for notifs
            window.addWindowFocusListener(
                object : WindowFocusListener {
                    override fun windowGainedFocus(e: WindowEvent) {
                        println("Window gained focus")
                    }

                    override fun windowLostFocus(e: WindowEvent) {
                        println("Window lost focus")
                    }
                }
            )

            window.minimumSize = Dimension(300, 600)

            AppTheme { App(root) }
        }
    }
}
