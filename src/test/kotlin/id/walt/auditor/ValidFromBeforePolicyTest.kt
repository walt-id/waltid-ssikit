package id.walt.auditor

import id.walt.vclib.vclist.VerifiableDiploma
import id.walt.vclib.vclist.VerifiablePresentation
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals

class ValidFromBeforePolicyTest : StringSpec({

    val validFromBeforePolicy = ValidFromBeforePolicy()

    "returns true when valid from is before current" {
        val vc = VerifiableDiploma(validFrom = "2019-06-22T14:11:44Z")
        assertEquals(true, validFromBeforePolicy.verify(vc))
    }

    "returns false when valid from is in the future" {
        val vc = VerifiableDiploma(validFrom = "3999-06-22T14:11:44Z")
        assertEquals(false, validFromBeforePolicy.verify(vc))
    }

    "returns false when vc is not a presentation and valid from is null" {
        val vc = VerifiableDiploma()
        assertEquals(false, validFromBeforePolicy.verify(vc))
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentation(verifiableCredential = listOf(VerifiableDiploma()))
        assertEquals(true, validFromBeforePolicy.verify(vp))
    }
})
