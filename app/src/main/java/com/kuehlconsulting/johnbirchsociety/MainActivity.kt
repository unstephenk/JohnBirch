package com.kuehlconsulting.johnbirchsociety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import com.kuehlconsulting.johnbirchsociety.ui.theme.JohnBirchSocietyTheme
import com.kuehlconsulting.johnbirchsociety.ui.rssfeed.RssFeedScreen
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.kuehlconsulting.johnbirchsociety.ui.player.PlayerScreen
import java.io.File

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var nowPlayingRef by rememberSaveable { mutableStateOf<String?>(null) }

            JohnBirchSocietyTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("John Birch Society") },
                            navigationIcon = {
                                IconButton(onClick = { /* Handle menu icon click, e.g., open a Drawer */ }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    // The content of your app, which will be the RssFeedScreen for now
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding), // Apply the padding from Scaffold
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (nowPlayingRef == null) {
                            // Main list
                            RssFeedScreen(
                                onNavigateToPlayer = { fileRef ->
                                    if (fileRef.isBlank()) {
                                        Toast.makeText(this, "No file to play", Toast.LENGTH_SHORT).show()
                                    } else {
                                        nowPlayingRef = fileRef
                                    }
                                }
                            )
                        } else {
                            // Full‑screen player
                            PlayerScreen(
                                ref = nowPlayingRef!!,
                                onClose = { nowPlayingRef = null }
                            )
                        }
                    }
                }
            }
        }
    }
}