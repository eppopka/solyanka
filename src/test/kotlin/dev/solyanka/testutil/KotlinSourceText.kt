/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.testutil

import java.io.File
import java.nio.file.Path

internal fun File.readSourceText() = KotlinSourceText(readText(), extension.equals("kt", ignoreCase = true))

internal fun Path.readSourceText() = toFile().readSourceText()

internal class KotlinSourceText(
    private val text: String,
    private val normalizeKotlin: Boolean,
) : CharSequence {
    private val tokens by lazy { if (normalizeKotlin) tokenize(text) else listOf(Token(text, 0, text.length)) }

    override val length get() = text.length

    override fun get(index: Int) = text[index]

    override fun subSequence(startIndex: Int, endIndex: Int) = text.subSequence(startIndex, endIndex)

    operator fun contains(sequence: CharSequence, ignoreCase: Boolean = false): Boolean =
        find(sequence.toString(), ignoreCase) != null

    fun indexOf(sequence: String, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
        val match = find(sequence, ignoreCase) ?: return -1
        return match.first.takeIf { it >= startIndex } ?: -1
    }

    fun substringAfter(delimiter: String, missingDelimiterValue: String = text): KotlinSourceText {
        val match = find(delimiter) ?: return KotlinSourceText(missingDelimiterValue, normalizeKotlin)
        val end = if (normalizeKotlin) tokens[match.second - 1].end else match.second
        return KotlinSourceText(text.substring(end), normalizeKotlin)
    }

    fun substringBefore(delimiter: String, missingDelimiterValue: String = text): KotlinSourceText {
        val match = find(delimiter) ?: return KotlinSourceText(missingDelimiterValue, normalizeKotlin)
        val start = if (normalizeKotlin) tokens[match.first].start else match.first
        return KotlinSourceText(text.substring(0, start), normalizeKotlin)
    }

    fun replace(oldValue: String, newValue: String, ignoreCase: Boolean = false) =
        KotlinSourceText(text.replace(oldValue, newValue, ignoreCase), normalizeKotlin)

    override fun toString() = text

    override fun equals(other: Any?) =
        other is KotlinSourceText && normalizeKotlin == other.normalizeKotlin && tokenValues() == other.tokenValues()

    override fun hashCode() = tokenValues().hashCode()

    private fun find(sequence: String, ignoreCase: Boolean = false): Pair<Int, Int>? {
        if (!normalizeKotlin) {
            val start = text.indexOf(sequence, ignoreCase = ignoreCase)
            return if (start < 0) null else start to start + sequence.length
        }
        val expected = tokenize(sequence).map(Token::value)
        if (expected.isEmpty()) return 0 to 0
        val source = tokens
        val limit = source.size - expected.size
        for (start in 0..limit) {
            val matches = expected.indices.all { index ->
                source[start + index].value.equals(expected[index], ignoreCase)
            }
            if (matches) return start to start + expected.size
        }
        return null
    }

    private fun tokenValues() = tokens.map(Token::value)

    private data class Token(val value: String, val start: Int, val end: Int)

    private companion object {
        private val operators =
            listOf(
                "!==", "===", ">>>", "<<=", ">>=", "..<", "...", "::", "->", "?.", "?:", "!!",
                "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=", "%=",
                "..", "<<", ">>", "&=", "|=", "^=",
            )

        private fun tokenize(source: String): List<Token> {
            val result = mutableListOf<Token>()
            var index = 0
            while (index < source.length) {
                val start = index
                val char = source[index]
                when {
                    char.isWhitespace() -> index++
                    source.startsWith("//", index) -> {
                        index = source.indexOf('\n', index + 2).let { if (it < 0) source.length else it + 1 }
                    }
                    source.startsWith("/*", index) -> {
                        index += 2
                        var depth = 1
                        while (index < source.length && depth > 0) {
                            when {
                                source.startsWith("/*", index) -> { depth++; index += 2 }
                                source.startsWith("*/", index) -> { depth--; index += 2 }
                                else -> index++
                            }
                        }
                    }
                    source.startsWith("\"\"\"", index) -> {
                        index += 3
                        val end = source.indexOf("\"\"\"", index)
                        index = if (end < 0) source.length else end + 3
                        result += Token(source.substring(start, index), start, index)
                    }
                    char == '"' || char == '\'' -> {
                        val quote = char
                        index++
                        while (index < source.length) {
                            if (source[index] == '\\') index = (index + 2).coerceAtMost(source.length)
                            else if (source[index++] == quote) break
                        }
                        result += Token(source.substring(start, index), start, index)
                    }
                    char == '`' -> {
                        index++
                        while (index < source.length && source[index] != '`') index++
                        if (index < source.length) index++
                        result += Token(source.substring(start, index), start, index)
                    }
                    char == '.' && index + 1 < source.length && source[index + 1].isDigit() -> {
                        index = numberEnd(source, index + 1)
                        result += Token("0" + source.substring(start, index), start, index)
                    }
                    char.isDigit() -> {
                        index = numberEnd(source, index)
                        result += Token(source.substring(start, index), start, index)
                    }
                    char.isLetter() || char == '_' -> {
                        index++
                        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
                        result += Token(source.substring(start, index), start, index)
                    }
                    else -> {
                        val operator = operators.firstOrNull { source.startsWith(it, index) }
                        index += operator?.length ?: 1
                        result += Token(source.substring(start, index), start, index)
                    }
                }
            }
            return result
        }

        private fun numberEnd(source: String, start: Int): Int {
            var index = start
            while (index < source.length) {
                val char = source[index]
                when {
                    char.isLetterOrDigit() || char == '_' -> index++
                    char == '.' && index + 1 < source.length && source[index + 1] != '.' -> index++
                    else -> return index
                }
            }
            return index
        }
    }
}
