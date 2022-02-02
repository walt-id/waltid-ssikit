package id.walt.services.oidc

import id.walt.model.oidc.Claims
import id.walt.model.oidc.CredentialClaim
import id.walt.model.oidc.OIDCIssuer
import id.walt.model.oidc.klaxon
import id.walt.vclib.credentials.VerifiableId
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.net.URI

class OIDC4VCTest : AnnotationSpec() {

    @Test
    fun testIssuanceRequest() {
        //val requestedCreds = listOf(CredentialClaim(type = VerifiableId.template!!().credentialSchema!!.id, manifest_id = null))
        //val req = OIDC4CI.createIssuanceRequest(URI.create("https://blank/"), requestedCreds)
        //val claims = klaxon.parse<Claims>(req.toParameters().get("claims")!!.first())
        //claims!!.credentials shouldContainExactly requestedCreds

        val issuer = OIDCIssuer("walt.id", "https://issuer.walt.id/issuer-api/oidc", "walt.id issuer")
        println(issuer.credentialManifests)
    }

}
