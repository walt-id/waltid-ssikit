package id.walt.common

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun toParamMap(paramString: String): Map<String, String> {
    val paramMap = HashMap<String, String>()
    val pairs = paramString.split("&")
    pairs.forEach {
        paramMap[it.substringBefore("=")] = URLDecoder.decode(it.substringAfter("="), StandardCharsets.UTF_8)
    }
    return paramMap
}

fun Any.prettyPrint(): String {

    var indentLevel = 0
    val indentWidth = 4

    fun padding() = "".padStart(indentLevel * indentWidth)

    val toString = toString()

    val stringBuilder = StringBuilder(toString.length)

    var i = 0
    while (i < toString.length) {
        when (val char = toString[i]) {
            '(', '[', '{' -> {
                indentLevel++
                stringBuilder.appendLine(char).append(padding())
            }

            ')', ']', '}' -> {
                indentLevel--
                stringBuilder.appendLine().append(padding()).append(char)
            }

            ',' -> {
                stringBuilder.appendLine(char).append(padding())
                // ignore space after comma as we have added a newline
                val nextChar = toString.getOrElse(i + 1) { char }
                if (nextChar == ' ') i++
            }

            else -> {
                stringBuilder.append(char)
            }
        }
        i++
    }

    return stringBuilder.toString()
}

fun urlEncode(str: String): String = URLEncoder.encode(str, StandardCharsets.UTF_8.toString())
