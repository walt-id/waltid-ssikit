/**
 * Created by Michael Avoyan on 4/5/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.extensions

import android.util.Base64
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

internal fun String.decode(): String = URLDecoder.decode(this, "UTF-8")

internal fun String.encode(): String = URLEncoder.encode(this, "UTF-8")

internal fun String.getUrlQueryParams(): Map<String, String>? {
    var map: MutableMap<String, String>? = null
    try {
        val params = this.split("[${Pattern.quote("?")}&]".toRegex()).toTypedArray()
        map = HashMap()
        for (param in params) {
            val pair = param.split("=".toRegex()).toTypedArray()
            if (pair.size == 2)
                map[pair[0]] = pair[1]
        }
    } catch (ex: Exception)
    {}
    return map
}

internal fun String.decodeBase64() = String(Base64.decode(this, 0))
internal fun String.encodeBase64(): String {
    var data: ByteArray? = null
    try {
        data = toByteArray(Charsets.UTF_8)
    } catch (e: UnsupportedEncodingException)
    { }
    return Base64.encodeToString(data, Base64.DEFAULT)
}

internal fun String.toDate(): Date? {
    val format = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    try {
        return format.parse(this)
    } catch (e: ParseException)
    { }
    return null
}

internal fun String.toJsonObject(): JSONObject? {
    return try {
        JSONObject(this)
    } catch (e: Exception) {
        null
    }
}

internal fun randomString(length: Int): String =
    List(length) {
        (('a'..'z') + ('A'..'Z') + ('0'..'9')).random()
    }.joinToString("")

