package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun OmnisSmartHelpDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Start, 1: FAQ, 2: AI Asistent, 3: Klávesové zkratky
    var searchQuery by remember { mutableStateOf("") }
    var assistantQuestion by remember { mutableStateOf("") }
    var assistantAnswer by remember { mutableStateOf("Jsem váš O.M.N.I.S. Smart Asistent. Zeptejte se mě na cokoliv ohledně ovládání, promptování nebo metodiky systému!") }

    val faqs = listOf(
        "Jak vytvořit nové vlákno?" to "V bočním menu (otevřete ikonou vlevo nahoře) klikněte na tlačítko '+ Nové vlákno'. Vlákno můžete také přejmenovat ikonou tužky.",
        "Jak přiložit soubor nebo fotku?" to "Ve spodním panelu chatu použijte tlačítko '+' pro přiložení souborů (PDF, texty, kódy) nebo ikonu fotoaparátu pro OCR skenování obrázků.",
        "Jak exportovat celou konverzaci?" to "V horní liště chatu klikněte na ikonu sdílení (Share) a zvolte export do PDF nebo Markdownu (.md).",
        "Co dělá Jistič (Circuit Breaker)?" to "Administrátorský prvek pro okamžité přerušení nebo bypass externích API volání v případě přetížení nebo výpadku."
    )

    val filteredFaqs = faqs.filter { 
        it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = OmnisPanelDark,
            border = BorderStroke(1.dp, OmnisCyan.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = OmnisCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, OmnisCyan)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                text = "O.M.N.I.S. SMART ASISTENT",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Interaktivní nápověda a průvodce systémem",
                                color = OmnisTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = OmnisTextLight)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(OmnisBgDark)
                        .horizontalScroll(rememberScrollState())
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("🚀 Rychlý Start", "❓ FAQ", "🤖 AI Poradce", "⚡ Tipy").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = index },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) OmnisCyan else Color.Transparent
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.Black else OmnisTextLight,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content based on selected tab
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> {
                            // Rychlý start
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    HelpCard(
                                        title = "1. Sémantické Promptování",
                                        description = "Zadávejte strukturované dotazy: [Doména] + [Akce] + [Kritérium]. Systém vás bude automaticky navádět nápovědou nad vstupním polem."
                                    )
                                }
                                item {
                                    HelpCard(
                                        title = "2. Správa Vláken & Historie",
                                        description = "V levém panelu můžete vytvářet nová vlákna, přejmenovávat existující (ikona tužky) nebo mazat nepotřebná konverzační vlákna."
                                    )
                                }
                                item {
                                    HelpCard(
                                        title = "3. Multimodální Vstupy & OCR",
                                        description = "Přikládejte dokumenty (PDF, kódy, Office) nebo skenujte text z fotek přes vestavěný OCR modul s náhledem."
                                    )
                                }
                                item {
                                    HelpCard(
                                        title = "4. Exporty do PDF / Markdownu",
                                        description = "Kompletní konverzace lze kdykoliv jedním kliknutím exportovat a sdílet jako formátovaný PDF dokument nebo Markdown (.md)."
                                    )
                                }
                            }
                        }
                        1 -> {
                            // FAQ & Search
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Hledat v nápovědě...", color = OmnisTextMuted, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OmnisCyan) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OmnisCyan,
                                        unfocusedBorderColor = OmnisBorderDark,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = OmnisBgDark,
                                        unfocusedContainerColor = OmnisBgDark
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(filteredFaqs) { faq ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = OmnisBgDark,
                                            border = BorderStroke(1.dp, OmnisBorderDark)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Q: ${faq.first}",
                                                    color = OmnisCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = faq.second,
                                                    color = OmnisTextLight,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // AI Poradce (Interactive Q&A simulator)
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = OmnisBgDark,
                                    border = BorderStroke(1.dp, OmnisBorderDark)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Psychology, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(18.dp))
                                            Text("Odpověď Asistenta:", color = OmnisCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Text(
                                            text = assistantAnswer,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Text("Rychlé dotazy na asistenta:", color = OmnisTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "Jak funguje 8D analýza?" to "8D Octagon analýza vyhodnocuje tenzory, metriky tření a doménové vazby v reálném čase pro maximální efektivitu kognitivního jádra.",
                                        "Jak přizpůsobit sémantickou bránu?" to "V administračním panelu můžete spínat Prompt Gateway a Jistič (Circuit Breaker) pro ochranu a validaci vstupů.",
                                        "Kde se ukládají data?" to "Všechna data, zprávy i artefakty jsou bezpečně uloženy lokálně v zašifrované Room databázi (v11) s podporou offline režimu."
                                    ).forEach { (q, a) ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    assistantQuestion = q
                                                    assistantAnswer = a
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = OmnisPanelDark,
                                            border = BorderStroke(1.dp, OmnisBorderDark)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(q, color = OmnisTextLight, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OmnisCyan, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Tipy & Klávesové zkratky
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                                item {
                                    HelpCard(
                                        title = "⚡ Pro-Tips pro Operátory",
                                        description = "• Používejte mikrofon pro rychlé hlasové diktování.\n• Přepínejte mezi kognitivními doménami kliknutím na horní filtr.\n• Pravidelně exportujte důležitá vlákna do Markdownu pro archivaci."
                                    )
                                }
                                item {
                                    HelpCard(
                                        title = "🛡️ Bezpečnostní Invarianty",
                                        description = "Systém striktně dodržuje RBAC oprávnění a chrání citlivá data operátorů lokální perzistencí bez neautorizovaného odesílání."
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OmnisCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Rozumím, zavřít nápovědu", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun HelpCard(title: String, description: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OmnisBgDark,
        border = BorderStroke(1.dp, OmnisBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = OmnisCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = description,
                color = OmnisTextLight,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
