package id.walt.auditor

import id.walt.vclib.credentials.VerifiableDiploma
import id.walt.vclib.credentials.VerifiablePresentation
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals

class IssuedDateBeforePolicyTest : StringSpec({

    val IssuedDateBeforePolicy = IssuedDateBeforePolicy()

    "returns true when issuance date is before current" {
        val vc = VerifiableDiploma(issued = "2019-06-22T14:11:44Z")
        assertEquals(true, IssuedDateBeforePolicy.verify(vc))
    }

    "returns false when issuance date is in the future" {
        val vc = VerifiableDiploma(issued = "3999-06-22T14:11:44Z")
        assertEquals(false, IssuedDateBeforePolicy.verify(vc))
    }

    "returns false when vc is not a presentation and issuance date is null" {
        val vc = VerifiableDiploma()
        assertEquals(false, IssuedDateBeforePolicy.verify(vc))
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentation(verifiableCredential = listOf(VerifiableDiploma()))
        assertEquals(true, IssuedDateBeforePolicy.verify(vp))
    }
})
