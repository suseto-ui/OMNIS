package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OmnisStyleSheet

/**
 * Vstupní lišta chatu O.M.N.I.S.
 * Stylizována pomocí tokenů OmnisStyleSheet (CSS-like).
 */
@Composable
fun ChatInputBar(
    inputQuery: String,
    onQueryChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    isLoading: Boolean,
    isOcrLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OmnisStyleSheet.Colors.PanelBackground,
        shape = OmnisStyleSheet.Shapes.input,
        border = OmnisStyleSheet.Borders.subtle,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttachFile,
                modifier = Modifier
                    .size(40.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Přiložit soubor",
                    tint = OmnisStyleSheet.Colors.CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onCameraClick,
                modifier = Modifier
                    .size(40.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Vyfotit/Obrázek",
                    tint = OmnisStyleSheet.Colors.CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .background(OmnisStyleSheet.Colors.InputBackground, OmnisStyleSheet.Shapes.input)
                    .border(1.dp, OmnisStyleSheet.Colors.BorderMuted, OmnisStyleSheet.Shapes.input)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = inputQuery,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = OmnisStyleSheet.Colors.TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Default
                    ),
                    cursorBrush = SolidColor(OmnisStyleSheet.Colors.CyanAccent),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputQuery.isNotBlank() && !isLoading && !isOcrLoading) {
                                onSend()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_input_field"),
                    decorationBox = { innerTextField ->
                        if (inputQuery.isEmpty()) {
                            Text(
                                text = "Zadejte cíl nebo dotaz...",
                                color = OmnisStyleSheet.Colors.TextMuted,
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(40.dp)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Hlasový vstup",
                    tint = OmnisStyleSheet.Colors.CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank() && !isLoading && !isOcrLoading) {
                        onSend()
                    }
                },
                enabled = inputQuery.isNotBlank() && !isLoading && !isOcrLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputQuery.isNotBlank() && !isLoading && !isOcrLoading)
                            OmnisStyleSheet.Colors.CyanAccent
                        else
                            OmnisStyleSheet.Colors.BorderMuted
                    )
                    .testTag("send_query_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Odeslat dotaz",
                    tint = if (inputQuery.isNotBlank() && !isLoading && !isOcrLoading)
                        Color.Black
                    else
                        OmnisStyleSheet.Colors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
