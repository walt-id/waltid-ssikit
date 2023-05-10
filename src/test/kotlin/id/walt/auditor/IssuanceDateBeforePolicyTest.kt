package id.walt.auditor

import id.walt.auditor.policies.IssuedDateBeforePolicy
import id.walt.credentials.w3c.PresentableCredential
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentationBuilder
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.Instant

class IssuedDateBeforePolicyTest : StringSpec({

    val issuedDateBeforePolicy = IssuedDateBeforePolicy()

    "returns true when issuance date is before current" {
        val vc = W3CCredentialBuilder().setIssued(Instant.parse("2019-06-22T14:11:44Z")).build()
        assertTrue(issuedDateBeforePolicy.verify(vc).isSuccess)
    }

    "returns false when issuance date is in the future" {
        val vc = W3CCredentialBuilder().setIssued(Instant.parse("3999-06-22T14:11:44Z")).build()
        assertTrue(issuedDateBeforePolicy.verify(vc).isFailure)
    }

    "returns false when vc is not a presentation and issuance date is null" {
        val vc = W3CCredentialBuilder().build()
        assertTrue(issuedDateBeforePolicy.verify(vc).isFailure)
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentationBuilder().setVerifiableCredentials(listOf(PresentableCredential(VerifiableCredential()))).build()
        assertTrue(issuedDateBeforePolicy.verify(vp).isSuccess)
    }
})
