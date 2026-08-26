package com.rastreadorfrota.poc.sqlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rastreadorfrota.poc.sqlite.ui.theme.PocScreen
import com.rastreadorfrota.poc.sqlite.ui.theme.SqliteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SqliteTheme {
                PocScreen()
            }
        }
    }
}