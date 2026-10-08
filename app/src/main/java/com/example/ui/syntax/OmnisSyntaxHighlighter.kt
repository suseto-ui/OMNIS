package com.example.ui.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.util.regex.Pattern

/**
 * Prism-style Syntax Highlighter pro Jetpack Compose kódové bloky.
 * Poskytuje pokročilé tokenové barvení pro Kotlin, Python, SQL, JSON, JavaScript/TypeScript, Bash a XML.
 */
object OmnisSyntaxHighlighter {

    // Prism Dark / One Dark Pro tokenová paleta
    private val COLOR_KEYWORD = Color(0xFFF43F5E) // Růžovo-červená
    private val COLOR_TYPE = Color(0xFFA855F7)    // Fialová
    private val COLOR_FUNCTION = Color(0xFF38BDF8) // Světle modrá / Cyan
    private val COLOR_STRING = Color(0xFF10B981)   // Zelená
    private val COLOR_NUMBER = Color(0xFFF59E0B)   // Jantarově oranžová
    private val COLOR_COMMENT = Color(0xFF64748B)  // Tlumená šedá
    private val COLOR_PUNCTUATION = Color(0xFF94A3B8) // Světlá šedá
    private val COLOR_DEFAULT = Color(0xFFE2E8F0)  // Výchozí text

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "enum", "val", "var",
        "fun", "return", "if", "else", "when", "for", "while", "do", "try", "catch",
        "finally", "throw", "override", "private", "public", "protected", "internal",
        "data", "sealed", "companion", "lazy", "by", "is", "in", "as", "suspend", "lateinit"
    )

    private val PYTHON_KEYWORDS = setOf(
        "import", "from", "def", "class", "return", "if", "elif", "else", "for", "while",
        "try", "except", "finally", "raise", "with", "as", "async", "await", "lambda",
        "yield", "pass", "break", "continue", "in", "is", "not", "and", "or", "global"
    )

    private val SQL_KEYWORDS = setOf(
        "select", "from", "where", "insert", "into", "update", "delete", "create", "table",
        "alter", "drop", "index", "join", "inner", "left", "right", "outer", "group", "by",
        "order", "having", "limit", "offset", "union", "values", "as", "on", "and", "or",
        "not", "null", "primary", "key", "foreign", "references", "distinct", "desc", "asc"
    )

    private val JS_KEYWORDS = setOf(
        "import", "export", "from", "const", "let", "var", "function", "return", "if",
        "else", "switch", "case", "default", "for", "while", "try", "catch", "finally",
        "throw", "async", "await", "class", "extends", "new", "this", "typeof", "interface", "type"
    )

    /**
     * Zvýrazní syntaxi předaného kódu a vrátí AnnotatedString.
     */
    fun highlight(code: String, language: String): AnnotatedString {
        val lang = language.trim().lowercase()
        return when {
            lang in listOf("json") -> highlightJson(code)
            lang in listOf("sql") -> highlightGenericLanguage(code, SQL_KEYWORDS)
            lang in listOf("python", "py") -> highlightGenericLanguage(code, PYTHON_KEYWORDS)
            lang in listOf("javascript", "typescript", "js", "ts", "tsx", "jsx") -> highlightGenericLanguage(code, JS_KEYWORDS)
            else -> highlightGenericLanguage(code, KOTLIN_KEYWORDS) // Výchozí Kotlin / obecný kód
        }
    }

    private fun highlightJson(json: String): AnnotatedString {
        return buildAnnotatedString {
            // Regex pro klíče "key":, hodnoty řetězců, čísla, booleany
            val pattern = Pattern.compile("(\"[^\"]*\")\\s*(:)|(\"[^\"]*\")|(-?\\d+\\.?\\d*)|(true|false|null)|([{}\\[\\],])")
            val matcher = pattern.matcher(json)
            var cursor = 0

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                if (start > cursor) {
                    append(json.substring(cursor, start))
                }

                when {
                    // JSON Klíč: "key":
                    matcher.group(1) != null && matcher.group(2) != null -> {
                        pushStyle(SpanStyle(color = COLOR_FUNCTION, fontWeight = FontWeight.SemiBold))
                        append(matcher.group(1).orEmpty())
                        pop()
                        pushStyle(SpanStyle(color = COLOR_PUNCTUATION))
                        append(":")
                        pop()
                    }
                    // JSON Hodnota řetězec: "value"
                    matcher.group(3) != null -> {
                        pushStyle(SpanStyle(color = COLOR_STRING))
                        append(matcher.group(3).orEmpty())
                        pop()
                    }
                    // Číslo
                    matcher.group(4) != null -> {
                        pushStyle(SpanStyle(color = COLOR_NUMBER, fontWeight = FontWeight.Bold))
                        append(matcher.group(4).orEmpty())
                        pop()
                    }
                    // Booleany & null
                    matcher.group(5) != null -> {
                        pushStyle(SpanStyle(color = COLOR_KEYWORD, fontWeight = FontWeight.Bold))
                        append(matcher.group(5).orEmpty())
                        pop()
                    }
                    // Závorky a čárky
                    matcher.group(6) != null -> {
                        pushStyle(SpanStyle(color = COLOR_PUNCTUATION))
                        append(matcher.group(6).orEmpty())
                        pop()
                    }
                }
                cursor = end
            }

            if (cursor < json.length) {
                append(json.substring(cursor))
            }
        }
    }

    private fun highlightGenericLanguage(code: String, keywords: Set<String>): AnnotatedString {
        return buildAnnotatedString {
            // Regex pro: 1) komentáře, 2) řetězce, 3) anotace, 4) čísla, 5) slova (klíčová slova vs funkce vs typy)
            val regex = "(//[^\n]*|/\\*[\\s\\S]*?\\*/|#[^\n]*)|(\"[^\"]*\"|'[^']*'|`[^`]*`)|(@[a-zA-Z_0-9]+)|(\\b\\d+\\.?\\d*[fFL]?\\b)|([a-zA-Z_][a-zA-Z0-9_]*)|([{}()\\[\\],;=+\\-*/<>!&|])"
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(code)
            var cursor = 0

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                if (start > cursor) {
                    append(code.substring(cursor, start))
                }

                when {
                    // Komentář
                    matcher.group(1) != null -> {
                        pushStyle(SpanStyle(color = COLOR_COMMENT, fontStyle = FontStyle.Italic))
                        append(matcher.group(1).orEmpty())
                        pop()
                    }
                    // Řetězec
                    matcher.group(2) != null -> {
                        pushStyle(SpanStyle(color = COLOR_STRING))
                        append(matcher.group(2).orEmpty())
                        pop()
                    }
                    // Anotace / Dekorátor (@Composable, @Override)
                    matcher.group(3) != null -> {
                        pushStyle(SpanStyle(color = COLOR_TYPE, fontWeight = FontWeight.SemiBold))
                        append(matcher.group(3).orEmpty())
                        pop()
                    }
                    // Číslo
                    matcher.group(4) != null -> {
                        pushStyle(SpanStyle(color = COLOR_NUMBER, fontWeight = FontWeight.Bold))
                        append(matcher.group(4).orEmpty())
                        pop()
                    }
                    // Slovo (Klíčové slovo, Typ nebo Funkce)
                    matcher.group(5) != null -> {
                        val word = matcher.group(5)!!
                        val lowerWord = word.lowercase()
                        when {
                            keywords.contains(lowerWord) -> {
                                pushStyle(SpanStyle(color = COLOR_KEYWORD, fontWeight = FontWeight.Bold))
                                append(word)
                                pop()
                            }
                            word[0].isUpperCase() -> {
                                // Třída / Typ (např. String, OmnisRecord, List)
                                pushStyle(SpanStyle(color = COLOR_TYPE, fontWeight = FontWeight.SemiBold))
                                append(word)
                                pop()
                            }
                            // Následuje závorka '(' => Volání funkce / metody
                            end < code.length && code[end] == '(' -> {
                                pushStyle(SpanStyle(color = COLOR_FUNCTION))
                                append(word)
                                pop()
                            }
                            else -> {
                                pushStyle(SpanStyle(color = COLOR_DEFAULT))
                                append(word)
                                pop()
                            }
                        }
                    }
                    // Operátory & interpunkce
                    matcher.group(6) != null -> {
                        pushStyle(SpanStyle(color = COLOR_PUNCTUATION))
                        append(matcher.group(6).orEmpty())
                        pop()
                    }
                }
                cursor = end
            }

            if (cursor < code.length) {
                append(code.substring(cursor))
            }
        }
    }
}
