package id.walt.signatory

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import kotlinx.serialization.Serializable
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.util.encoders.Base32.toBase32String
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import java.time.Instant
import kotlin.io.path.*

object RevocationService {

    private val klaxon = Klaxon()
    private val revokedPath = Path("revoked.json").apply {
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
        val token = getRevocationToken(baseToken)
        val revoked = getRevokedList().toMutableList().apply {
            add(RevocationResult(token, true, Instant.now().toEpochMilli()))
        }
        setRevokedList(RevocationList(revoked))
    }

    fun getRevocationToken(baseToken: String) = toBase32String(DigestUtils.sha256(baseToken)).replace("=", "")
}
