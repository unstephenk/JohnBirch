package com.kuehlconsulting.johnbirchsociety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kuehlconsulting.johnbirchsociety.ui.theme.JohnBirchSocietyTheme
import com.kuehlconsulting.johnbirchsociety.ui.rssfeed.RssFeedScreen // Import your RssFeedScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JohnBirchSocietyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RssFeedScreen() // Display your RSS feed screen
                }
                }
            }
        }
}