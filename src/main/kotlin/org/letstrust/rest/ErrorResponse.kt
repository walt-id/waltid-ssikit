package org.letstrust.rest

data class ErrorResponse(
    val message: String,
    val status: Int,
    //val details: Map<String, String>? = emptyMap()
)
