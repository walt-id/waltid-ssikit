package id.walt.auditor

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentationBuilder
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Instant

class ExpirationDateBeforePolicyTest : StringSpec({

    val expirationDateAfterPolicy = ExpirationDateAfterPolicy()

    "returns true when expiration date is after current" {
        val vc = W3CCredentialBuilder().setExpirationDate(Instant.parse("3999-06-22T14:11:44Z")).build()
        assertEquals(true, expirationDateAfterPolicy.verify(vc))
    }

    "returns false when expiration date is in the future" {
        val vc = W3CCredentialBuilder().setExpirationDate(Instant.parse("2019-06-22T14:11:44Z")).build()
        assertEquals(false, expirationDateAfterPolicy.verify(vc))
    }

    "returns true when vc is not a presentation and expiration date is null" {
        val vc = W3CCredentialBuilder().build()
        assertEquals(true, expirationDateAfterPolicy.verify(vc))
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentationBuilder().setVerifiableCredentials(listOf(VerifiableCredential())).build()
        assertEquals(true, expirationDateAfterPolicy.verify(vp))
    }
})
