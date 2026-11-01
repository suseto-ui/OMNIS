package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.OmnisMainScreen
import com.example.ai.configureAppCheck
import com.example.ui.OmnisViewModel
import com.example.ui.PdfExportEngine
import com.example.ui.SpeechController
import com.example.ui.theme.OmnisTheme

class MainActivity : ComponentActivity() {
    private lateinit var speechController: SpeechController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        speechController = SpeechController(this)
        speechController.initialize()
        com.example.ui.chat.ChatDisplayPreferences.init(this)

        setContent {
            OmnisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: OmnisViewModel = viewModel()
                    OmnisMainScreen(
                        viewModel = viewModel,
                        onSpeak = { text ->
                            speechController.speak(text)
                        },
                        onExportPdf = { record ->
                            PdfExportEngine.exportToPdf(this, record, packageName)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechController.shutdown()
    }
}

