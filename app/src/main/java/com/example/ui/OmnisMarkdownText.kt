package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Komponenta pro pokročilé a vysoce čitelné vykreslování Markdownu v odpovědích O.M.N.I.S.
 * Podporuje:
 * - H1, H2, H3 nadpisy s vizuálním oddělovačem
 * - Víceřádkové kódové bloky (```lang ... ```) se zvýrazněním pozadí a tlačítkem kopírování
 * - Inline kód (`code`)
 * - Seznamy s odrážkami (•, -, *)
 * - Zvýrazněný tučný text (**bold**) a kurzívu (*italic*)
 * - Zvýraznění klíčových kognitivních značek (např. [SÉMANTICKÉ ZADÁNÍ], SYSTEM, POZOR)
 */
@Composable
fun OmnisMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: Int = 13
) {
    val context = LocalContext.current
    val blocks = parseMarkdownBlocks(text)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val (headerSize, headerColor) = when (block.level) {
                        1 -> 16.sp to OmnisCyan
                        2 -> 14.5.sp to OmnisEmerald
                        else -> 13.5.sp to Color.White
                    }
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = block.text,
                            color = headerColor,
                            fontSize = headerSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        HorizontalDivider(
                            color = headerColor.copy(alpha = 0.3f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, OmnisBorderDark),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = OmnisCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = block.language.ifBlank { "KÓD" }.uppercase(),
                                        color = OmnisCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("code", block.code))
                                        Toast.makeText(context, "Kód zkopírován do schránky", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Kopírovat kód",
                                        tint = OmnisTextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = block.code,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = OmnisCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = renderInlineStyles(block.text, textColor),
                            color = textColor,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize + 6).sp
                        )
                    }
                }
                is MarkdownBlock.Callout -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = OmnisPanelDark,
                        border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(OmnisCyan, RoundedCornerShape(2.dp))
                            )
                            Text(
                                text = renderInlineStyles(block.text, textColor),
                                color = textColor,
                                fontSize = (fontSize - 1).sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = renderInlineStyles(block.text, textColor),
                        color = textColor,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize + 6).sp
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class Callout(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var inCodeBlock = false
    var codeLang = ""
    val codeBuilder = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // Konec kódového bloku
                blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuilder.toString().trimEnd()))
                codeBuilder.clear()
                inCodeBlock = false
            } else {
                // Začátek kódového bloku
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBuilder.append(line).append("\n")
            continue
        }

        if (trimmed.isEmpty()) {
            continue
        }

        when {
            trimmed.startsWith("### ") -> blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
            trimmed.startsWith("## ") -> blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
            trimmed.startsWith("# ") -> blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> blocks.add(MarkdownBlock.BulletItem(trimmed.substring(2).trim()))
            trimmed.startsWith("> ") -> blocks.add(MarkdownBlock.Callout(trimmed.removePrefix("> ").trim()))
            else -> blocks.add(MarkdownBlock.Paragraph(line))
        }
    }

    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuilder.toString().trimEnd()))
    }

    return blocks
}

private fun renderInlineStyles(raw: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val text = raw

        // Regex pro zachycení inline code `...`, bold **...**, nebo [TAG]
        val pattern = java.util.regex.Pattern.compile("(`([^`]+)`)|(\\*\\*([^*]+)\\*\\*)|(\\[([A-ZÁČĎÉĚÍŇÓŘŠŤÚŮÝŽ_\\s]{3,})\\])")
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > cursor) {
                append(text.substring(cursor, start))
            }

            when {
                // Inline code `...`
                matcher.group(1) != null -> {
                    val codeContent = matcher.group(2) ?: ""
                    pushStyle(
                        SpanStyle(
                            color = OmnisCyan,
                            background = Color(0xFF1E293B),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(" $codeContent ")
                    pop()
                }
                // Bold **...**
                matcher.group(3) != null -> {
                    val boldContent = matcher.group(4) ?: ""
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                    append(boldContent)
                    pop()
                }
                // Tag [TAG]
                matcher.group(5) != null -> {
                    val tagContent = matcher.group(6) ?: ""
                    pushStyle(
                        SpanStyle(
                            color = OmnisEmerald,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    append("[$tagContent]")
                    pop()
                }
            }
            cursor = end
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
