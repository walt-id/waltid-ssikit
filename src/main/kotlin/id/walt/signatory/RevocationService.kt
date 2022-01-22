package id.walt.signatory

import com.beust.klaxon.*
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.util.encoders.Base32.toBase32String
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object RevocationService {

    private val klaxon = Klaxon()
    private val revokedPath = Path("revoked.json").apply {
        if (!exists())
            writeText(klaxon.toJsonString(RevocationList(emptyList())))
    }

    data class RevocationList(val revokedList: List<RevocationResult>)
    data class RevocationResult(
        val token: String,
        val isRevoked: Boolean,
        @Json(serializeNull = false) val timeOfRevocation: LocalDateTime? = null
    )

    private fun getRevokedList() = klaxon.parse<RevocationList>(revokedPath.readText())!!.revokedList
    private fun setRevokedList(revoked: RevocationList) = revokedPath.writeText(klaxon.toJsonString(revoked))

    fun checkRevoked(token: String): RevocationResult {
        return getRevokedList().firstOrNull { it.token == token } ?: return RevocationResult(token, false)
    }

    fun revokeToken(baseToken: String) { // UUIDUUID -> SHA256-Token (base32)
        if (baseToken.length != 72) throw IllegalArgumentException("base token has to have 72 chars (uuiduuid)")
        val token = getRevocationToken(baseToken)
        val revoked = getRevokedList().toMutableList().apply { add(RevocationResult(token, true, LocalDateTime.now())) }
        setRevokedList(RevocationList(revoked))
    }

    fun getRevocationToken(baseToken: String) = toBase32String(DigestUtils.sha256(baseToken))
}

fun main() {
    println("Check revoked: ")
    println(RevocationService.checkRevoked("abc"))

    val baseToken = UUID.randomUUID().toString() + UUID.randomUUID().toString()
    println("New base token: $baseToken")
    println("Revocation token: ${RevocationService.getRevocationToken(baseToken)}")

    println("Revoke:")
    RevocationService.revokeToken(baseToken)

    println("Check revoked:")
    println(RevocationService.checkRevoked(RevocationService.getRevocationToken(baseToken)))
}
