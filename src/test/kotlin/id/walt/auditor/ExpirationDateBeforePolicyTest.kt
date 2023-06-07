package id.walt.auditor

import id.walt.auditor.policies.ExpirationDateAfterPolicy
import id.walt.credentials.w3c.PresentableCredential
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentationBuilder
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.Instant

class ExpirationDateBeforePolicyTest : StringSpec({

    val expirationDateAfterPolicy = ExpirationDateAfterPolicy()

    "returns true when expiration date is after current" {
        val vc = W3CCredentialBuilder().setExpirationDate(Instant.parse("3999-06-22T14:11:44Z")).build()
        assertTrue(expirationDateAfterPolicy.verify(vc).isSuccess)
    }

    "returns false when expiration date is in the future" {
        val vc = W3CCredentialBuilder().setExpirationDate(Instant.parse("2019-06-22T14:11:44Z")).build()
        assertTrue(expirationDateAfterPolicy.verify(vc).isFailure)
    }

    "returns true when vc is not a presentation and expiration date is null" {
        val vc = W3CCredentialBuilder().build()
        assertTrue(expirationDateAfterPolicy.verify(vc).isSuccess)
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentationBuilder().setVerifiableCredentials(listOf(PresentableCredential(VerifiableCredential()))).build()
        assertTrue(expirationDateAfterPolicy.verify(vp).isSuccess)
    }
})
