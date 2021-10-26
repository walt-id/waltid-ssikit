package id.walt.auditor

import id.walt.vclib.vclist.VerifiableDiploma
import id.walt.vclib.vclist.VerifiablePresentation
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals

class IssuanceDateBeforePolicyTest : StringSpec({

    val issuanceDateBeforePolicy = IssuanceDateBeforePolicy()

    "returns true when issuance date is before current" {
        val vc = VerifiableDiploma(issuanceDate = "2019-06-22T14:11:44Z")
        assertEquals(true, issuanceDateBeforePolicy.verify(vc))
    }

    "returns false when issuance date is in the future" {
        val vc = VerifiableDiploma(issuanceDate = "3999-06-22T14:11:44Z")
        assertEquals(false, issuanceDateBeforePolicy.verify(vc))
    }

    "returns false when vc is not a presentation and issuance date is null" {
        val vc = VerifiableDiploma()
        assertEquals(false, issuanceDateBeforePolicy.verify(vc))
    }

    "returns always true when vc is a presentation" {
        val vp = VerifiablePresentation(verifiableCredential = listOf(VerifiableDiploma()))
        assertEquals(true, issuanceDateBeforePolicy.verify(vp))
    }
})
