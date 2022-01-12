package id.walt.model.dif

import com.beust.klaxon.Json

data class Issuer (val id: String, @Json(serializeNull = false) val name: String?)
