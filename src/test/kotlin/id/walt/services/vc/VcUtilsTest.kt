package id.walt.services.vc

import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.vclist.VerifiableDiploma
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals

class VcUtilsTest : StringSpec({
    val date = "2019-06-22T14:11:44Z"

    "getIssuanceDate returns null when it does not exist" {
        assertEquals(null, VcUtils.getIssuanceDate(VerifiableDiploma()))
    }

    "getIssuanceDate returns the issuance date when it exists" {
        assertEquals(date, VcUtils.getIssuanceDate(VerifiableDiploma(issuanceDate = date)))
    }

    "getValidFrom returns null when it does not exist" {
        assertEquals(null, VcUtils.getValidFrom(VerifiableDiploma()))
    }

    "getValidFrom returns valid from when it exists" {
        assertEquals(date, VcUtils.getValidFrom(VerifiableDiploma(validFrom = date)))
    }

    "getExpirationDate returns null when it does not exist" {
        assertEquals(null, VcUtils.getValidFrom(VerifiableDiploma()))
    }

    "getExpirationDate returns the expiration date when it exists" {
        assertEquals(date, VcUtils.getExpirationDate(VerifiableDiploma(expirationDate = date)))
    }

    "getCredentialSchema returns the credentialSchema attribute value when it exists" {
        assertEquals(null, VcUtils.getCredentialSchema(VerifiableDiploma()))
        CredentialSchema(id = "id", type = "type").let {
            assertEquals(it, VcUtils.getCredentialSchema(VerifiableDiploma(credentialSchema = it)))
        }
    }
})
