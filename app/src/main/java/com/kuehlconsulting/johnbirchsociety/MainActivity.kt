package com.kuehlconsulting.johnbirchsociety

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.kuehlconsulting.johnbirchsociety.ui.theme.JohnBirchSocietyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JohnBirchSocietyTheme {
                    PodcastRow()
                }
            }
        }
}


@Composable
fun PodcastRow() {
    var selected by remember { mutableStateOf(false)}

    Row {
        Text(
            text = "This is a row"
        )
        RadioButton(
            selected = selected,
            onClick = { selected = !selected})
    }
}