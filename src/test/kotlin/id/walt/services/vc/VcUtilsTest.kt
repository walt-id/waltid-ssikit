package id.walt.services.vc

import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.vclist.VerifiableDiploma
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

class VcUtilsTest : StringSpec({
    "getIssuanceDate returns null when it does not exist" {
        assertEquals(null, VcUtils.getIssuanceDate(VerifiableDiploma()))
    }

    "getIssuanceDate returns the issuance date when it exists" {
        val expected = Date(1561205504000)
        val actual = VcUtils.getIssuanceDate(VerifiableDiploma(issuanceDate = "2019-06-22T14:11:44Z"))
        assertEquals(expected, actual)
    }

    "getValidFrom returns null when it does not exist" {
        assertEquals(null, VcUtils.getValidFrom(VerifiableDiploma()))
    }

    "getValidFrom returns valid from when it exists" {
        val expected = Date(1561205504000)
        val actual = VcUtils.getValidFrom(VerifiableDiploma(validFrom = "2019-06-22T14:11:44Z"))
        assertEquals(expected, actual)
    }

    "getExpirationDate returns null when it does not exist" {
        assertEquals(null, VcUtils.getValidFrom(VerifiableDiploma()))
    }

    "getExpirationDate returns the expiration date when it exists" {
        val expected = Date(1561205504000)
        val actual = VcUtils.getExpirationDate(VerifiableDiploma(expirationDate = "2019-06-22T14:11:44Z"))
        assertEquals(expected, actual)
    }

    "getCredentialSchema returns the credentialSchema attribute value when it exists" {
        assertEquals(null, VcUtils.getCredentialSchema(VerifiableDiploma()))
        CredentialSchema(id = "id", type = "type").let {
            assertEquals(it, VcUtils.getCredentialSchema(VerifiableDiploma(credentialSchema = it)))
        }
    }
})
