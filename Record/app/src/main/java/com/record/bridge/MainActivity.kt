package com.record.bridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.record.bridge.ui.AppNav
import com.record.bridge.ui.theme.RecordTheme

val LocalApp = staticCompositionLocalOf<RecordApp> {
    error("RecordApp not provided")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RecordApp
        setContent {
            CompositionLocalProvider(LocalApp provides app) {
                RecordTheme(darkTheme = isSystemInDarkTheme()) {
                    AppNav()
                }
            }
        }
    }
}

