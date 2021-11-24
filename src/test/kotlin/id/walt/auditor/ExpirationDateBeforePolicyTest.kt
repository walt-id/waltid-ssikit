package id.walt.auditor

import id.walt.vclib.credentials.VerifiableDiploma
import id.walt.vclib.credentials.VerifiablePresentation
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals

class ExpirationDateBeforePolicyTest : StringSpec({

    val expirationDateAfterPolicy = ExpirationDateAfterPolicy()

    "returns true when expiration date is after current" {
        val vc = VerifiableDiploma(expirationDate = "3999-06-22T14:11:44Z")
        assertEquals(true, expirationDateAfterPolicy.verify(vc))
    }

    "returns false when expiration date is in the future" {
        val vc = VerifiableDiploma(expirationDate = "2019-06-22T14:11:44Z")
        assertEquals(false, expirationDateAfterPolicy.verify(vc))
    }

    "returns true when vc is not a presentation and expiration date is null" {
        val vc = VerifiableDiploma()
        assertEquals(true, expirationDateAfterPolicy.verify(vc))
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentation(verifiableCredential = listOf(VerifiableDiploma()))
        assertEquals(true, expirationDateAfterPolicy.verify(vp))
    }
})
