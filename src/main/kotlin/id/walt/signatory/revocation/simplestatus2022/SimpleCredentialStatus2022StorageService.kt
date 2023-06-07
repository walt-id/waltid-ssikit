package id.walt.signatory.revocation.simplestatus2022

import com.beust.klaxon.Klaxon
import id.walt.common.deriveRevocationToken
import id.walt.services.WaltIdServices
import id.walt.signatory.revocation.RevocationStatus
import id.walt.signatory.revocation.TokenRevocationStatus
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object SimpleCredentialStatus2022StorageService {

    private val klaxon = Klaxon()
    private val revokedPath = Path("${WaltIdServices.revocationDir}/revoked.json").apply {
        if (!exists()) writeText(klaxon.toJsonString(RevocationList(emptyList())))
    }

    private fun getRevokedList() = klaxon.parse<RevocationList>(revokedPath.readText())!!.revokedList
    private fun setRevokedList(revoked: RevocationList) = revokedPath.writeText(klaxon.toJsonString(revoked))

    fun clearRevocations() = setRevokedList(RevocationList(emptyList()))

    fun checkRevoked(token: String): RevocationStatus {
        val derivedToken = deriveRevocationToken(token)
        if (derivedToken.contains("-")) throw IllegalArgumentException("Revocation token contains '-', you probably didn't supply a derived revocation token, but a base token.")
        return getRevokedList().firstOrNull { (it as? TokenRevocationStatus)?.token == derivedToken } ?: return TokenRevocationStatus(token, false)
    }

    fun revokeToken(baseToken: String) { // UUIDUUID -> SHA256-Token (base32)
        if (baseToken.length != 72) throw IllegalArgumentException("base token has to have 72 chars (uuiduuid)")
        val token = deriveRevocationToken(baseToken)
        val revoked = getRevokedList().toMutableList().apply {
            add(TokenRevocationStatus(token, true, Instant.now().toEpochMilli()))
        }
        setRevokedList(RevocationList(revoked))
    }

    data class RevocationList(val revokedList: List<TokenRevocationStatus>)
}
