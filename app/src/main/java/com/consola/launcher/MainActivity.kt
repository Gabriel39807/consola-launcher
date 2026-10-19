package com.consola.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.consola.launcher.home.HomeScreen
import com.consola.launcher.ui.ConsolaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsolaTheme {
                HomeScreen()
            }
        }
    }
}
