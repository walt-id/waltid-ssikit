package org.letstrust.rest

data class ErrorResponse(
    val title: String,
    val status: Int,
    val details: Map<String, String>? = emptyMap()
)
