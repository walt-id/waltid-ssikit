package id.walt.auditor

import id.walt.auditor.policies.ValidFromBeforePolicy
import id.walt.credentials.w3c.PresentableCredential
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentationBuilder
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.Instant

class ValidFromBeforePolicyTest : StringSpec({

    val validFromBeforePolicy = ValidFromBeforePolicy()

    "returns true when valid from is before current" {
        val vc = W3CCredentialBuilder().setValidFrom(Instant.parse("2019-06-22T14:11:44Z")).build()
        assertTrue(validFromBeforePolicy.verify(vc).isSuccess)
    }

    "returns false when valid from is in the future" {
        val vc = W3CCredentialBuilder().setValidFrom(Instant.parse("3999-06-22T14:11:44Z")).build()
        assertTrue(validFromBeforePolicy.verify(vc).isFailure)
    }

    "returns false when vc is not a presentation and valid from is null" {
        val vc = W3CCredentialBuilder().build()
        assertTrue(validFromBeforePolicy.verify(vc).isFailure)
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentationBuilder().setVerifiableCredentials(listOf(PresentableCredential(VerifiableCredential()))).build()
        assertTrue(validFromBeforePolicy.verify(vp).isSuccess)
    }
})
