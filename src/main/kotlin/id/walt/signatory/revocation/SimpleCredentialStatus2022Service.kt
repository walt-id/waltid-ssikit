package id.walt.signatory.revocation

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import id.walt.common.deriveRevocationToken
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object SimpleCredentialStatus2022Service {

    private val klaxon = Klaxon()
    private val revokedPath = Path("data/revoked.json").apply {
        if (!exists())
            writeText(klaxon.toJsonString(RevocationList(emptyList())))
    }

    data class RevocationList(val revokedList: List<RevocationResult>)

    @Serializable
    data class RevocationResult(
        val token: String,
        val isRevoked: Boolean,
        @Json(serializeNull = false) val timeOfRevocation: Long? = null
    )

    private fun getRevokedList() = klaxon.parse<RevocationList>(revokedPath.readText())!!.revokedList
    private fun setRevokedList(revoked: RevocationList) = revokedPath.writeText(klaxon.toJsonString(revoked))

    fun clearRevocations() = setRevokedList(RevocationList(emptyList()))

    fun checkRevoked(token: String): RevocationResult {
        if (token.contains("-")) throw IllegalArgumentException("Revocation token contains '-', you probably didn't supply a derived revocation token, but a base token.")

        println(getRevokedList())
        return getRevokedList().firstOrNull { it.token == token } ?: return RevocationResult(token, false)
    }

    fun revokeToken(baseToken: String) { // UUIDUUID -> SHA256-Token (base32)
        if (baseToken.length != 72) throw IllegalArgumentException("base token has to have 72 chars (uuiduuid)")
        val token = deriveRevocationToken(baseToken)
        val revoked = getRevokedList().toMutableList().apply {
            add(RevocationResult(token, true, Instant.now().toEpochMilli()))
        }
        setRevokedList(RevocationList(revoked))
    }
}
