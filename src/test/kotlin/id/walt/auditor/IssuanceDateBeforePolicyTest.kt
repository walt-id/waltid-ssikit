package id.walt.auditor

import id.walt.vclib.credentials.VerifiableDiploma
import id.walt.vclib.credentials.VerifiablePresentation
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals

class IssuedDateBeforePolicyTest : StringSpec({

    val issuedDateBeforePolicy = IssuedDateBeforePolicy()

    "returns true when issued date is before current" {
        val vc = VerifiableDiploma(issued = "2019-06-22T14:11:44Z")
        assertEquals(true, issuedDateBeforePolicy.verify(vc))
    }

    "returns false when issued date is in the future" {
        val vc = VerifiableDiploma(issued = "3999-06-22T14:11:44Z")
        assertEquals(false, issuedDateBeforePolicy.verify(vc))
    }

    "returns false when vc is not a presentation and issued date is null" {
        val vc = VerifiableDiploma()
        assertEquals(false, issuedDateBeforePolicy.verify(vc))
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentation(verifiableCredential = listOf(VerifiableDiploma()))
        assertEquals(true, issuedDateBeforePolicy.verify(vp))
    }
})
