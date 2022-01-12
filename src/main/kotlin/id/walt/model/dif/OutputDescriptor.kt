package id.walt.model.dif

import com.beust.klaxon.Json

data class OutputDescriptor (
    val id: String,
    val schema: String,
    @Json(serializeNull = false) val name: String?
)
