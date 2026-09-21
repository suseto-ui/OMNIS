package com.example

import com.example.ui.OmnisMainScreen

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OmnisRecord
import com.example.TestSemanticChatView
import androidx.compose.material.icons.filled.Science
import com.example.ui.OmnisTab
import com.example.ui.OmnisViewModel
import com.example.ui.OctagonDashboard
import com.example.ui.theme.*
import java.io.FileOutputStream
import java.util.Locale
import com.example.ui.PdfExportEngine
import com.example.ui.SpeechController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: OmnisViewModel by viewModels()
    private lateinit var speechController: SpeechController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

