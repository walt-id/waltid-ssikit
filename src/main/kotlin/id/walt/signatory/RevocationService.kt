package id.walt.signatory

import java.time.LocalDateTime

object RevocationService {

    data class RevocationResult(val isRevoked: Boolean, val timeOfRevocation: LocalDateTime? = null)

    fun checkRevoked(token: String): RevocationResult {
        return RevocationResult(false)
    }
    fun revokeToken(token: String) {

    }
}
