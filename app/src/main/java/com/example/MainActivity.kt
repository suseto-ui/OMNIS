package com.example

import com.example.ui.OmnisMainScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.OmnisViewModel
import com.example.ui.theme.*
import com.example.ui.PdfExportEngine
import com.example.ui.SpeechController

class MainActivity : ComponentActivity() {
    private val viewModel: OmnisViewModel by viewModels()
    private lateinit var speechController: SpeechController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.localization.AppLocaleManager.initialize(this)
        speechController = SpeechController(this)
        speechController.initialize()
        enableEdgeToEdge()
        setContent {
            OmnisTheme {
                OmnisMainScreen(
                    viewModel = viewModel,
                    onSpeak = { text ->
                        speechController.speak(text)
                    },
                    onExportPdf = { record -> PdfExportEngine.exportToPdf(this, record, packageName) }
                )
            }
        }
    }

    override fun onDestroy() {
        speechController.shutdown()
        super.onDestroy()
    }
}

/**
 * Kept for unit & screenshot test compatibility.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        color = OmnisCyan,
        modifier = modifier.testTag("greeting_text")
    )
}

