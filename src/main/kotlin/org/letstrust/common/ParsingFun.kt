package org.letstrust.common

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.HashMap

fun toParamMap(paramString: String): Map<String, String> {
    val paramMap = HashMap<String, String>()
    val pairs = paramString.split("&")
    pairs.forEach { paramMap[it.substringBefore("=")] = URLDecoder.decode(it.substringAfter("="), StandardCharsets.UTF_8) }
    return paramMap
}
