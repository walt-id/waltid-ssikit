package id.walt.services.oidc

import id.walt.model.oidc.OIDCProvider
import io.kotest.core.spec.style.AnnotationSpec
import java.net.URI

class OIDC4VCTest : AnnotationSpec() {

    @Test
    fun testIssuanceRequest() {
        //val requestedCreds = listOf(CredentialClaim(type = VerifiableId.template!!().credentialSchema!!.id, manifest_id = null))
        //val req = OIDC4CI.createIssuanceRequest(URI.create("https://blank/"), requestedCreds)
        //val claims = klaxon.parse<Claims>(req.toParameters().get("claims")!!.first())
        //claims!!.credentials shouldContainExactly requestedCreds

        val issuer = OIDCProvider("walt.id", "https://issuer.walt.id/issuer-api/oidc", "walt.id issuer")
        //println(issuer.credentialManifests)
    }

    @Test
    fun testEbsiAuthRequestParsing() {
        val reqUri = "openid://?response_type=id_token&client_id=https%3A%2F%2Fapi.conformance.intebsi.xyz%2Fconformance%2Fv1%2Fverifier-mock%2Fauthentication-responses&scope=ebsi%20conformance%20mock&nonce=0f41579f-f012-489f-9715-ffeb2c744c6c&request=eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QiLCJraWQiOiJkaWQ6ZWJzaTp6MjFTcU1KNTNmWVM2OVZaUllUVUVEYWcvI2tleXMtMSJ9.eyJpYXQiOjE2NDQyMzUxNjcsImV4cCI6MTY0NDIzNTQ2NywiaXNzIjoiZGlkOmVic2k6ejIxU3FNSjUzZllTNjlWWlJZVFVFRGFnIiwic2NvcGUiOiJlYnNpIGNvbmZvcm1hbmNlIG1vY2siLCJyZXNwb25zZV90eXBlIjoiaWRfdG9rZW4iLCJyZXNwb25zZV9tb2RlIjoicG9zdCIsImNsaWVudF9pZCI6Imh0dHBzOi8vYXBpLmNvbmZvcm1hbmNlLmludGVic2kueHl6L2NvbmZvcm1hbmNlL3YxL3ZlcmlmaWVyLW1vY2svYXV0aGVudGljYXRpb24tcmVzcG9uc2VzIiwicmVkaXJlY3RfdXJpIjoiaHR0cHM6Ly9hcGkuY29uZm9ybWFuY2UuaW50ZWJzaS54eXovY29uZm9ybWFuY2UvdjEvdmVyaWZpZXItbW9jay9hdXRoZW50aWNhdGlvbi1yZXNwb25zZXMiLCJub25jZSI6IjBmNDE1NzlmLWYwMTItNDg5Zi05NzE1LWZmZWIyYzc0NGM2YyIsInJlZ2lzdHJhdGlvbiI6Im1vY2sgcmVnaXN0cmF0aW9uIiwiY2xhaW1zIjp7ImlkX3Rva2VuIjp7InZwX3Rva2VuIjp7InByZXNlbnRhdGlvbl9kZWZpbml0aW9uIjp7ImlucHV0X2Rlc2NyaXB0b3JzIjpbeyJzY2hlbWEiOnsidXJpIjoiaHR0cHM6Ly9hcGkucHJlcHJvZC5lYnNpLmV1L3RydXN0ZWQtc2NoZW1hcy1yZWdpc3RyeS92MS9zY2hlbWFzLzB4MTRiMDViOTIxM2RiZTdkMzQzZWMxZmUxZDNjOGM3MzlhM2YzZGM1YTU5YmFlNTVlYjM4ZmEwYzI5NTEyNGY0OSMifX1dfX19fX0.IsIKBZlcRf2vvEgwR2gxYeg79yefQ3F0vsboUAGl4bgRO1r_g1kmiN96aXXiCsOqkeoz6fe0KE_AxRE5p0o6FQ&claims=%7B%22id_token%22%3A%7B%22vp_token%22%3A%7B%22presentation_definition%22%3A%7B%22input_descriptors%22%3A%5B%7B%22schema%22%3A%7B%22uri%22%3A%22https%3A%2F%2Fapi.preprod.ebsi.eu%2Ftrusted-schemas-registry%2Fv1%2Fschemas%2F0x14b05b9213dbe7d343ec1fe1d3c8c739a3f3dc5a59bae55eb38fa0c295124f49%23%22%7D%7D%5D%7D%7D%7D%7D"

        val siopReq = OIDC4VPService(OIDCProvider("http://blank", "http://blank")).parseSIOPv2RequestUri(URI.create(reqUri))

        println(siopReq!!.redirect_uri)
        println(siopReq!!.claims.toJSONObject())
    }

    @Test
    fun testWaltAuthRequestParsing() {
        val reqUri = "http://localhost:3000/CredentialRequest/?response_type=id_token&response_mode=form_post&client_id=http%3A%2F%2Flocalhost%3A4000%2Fverifier-api%2Fverify%2Fb80f6828-ea05-4122-937d-b643bf4e2444&redirect_uri=http%3A%2F%2Flocalhost%3A4000%2Fverifier-api%2Fverify%2Fb80f6828-ea05-4122-937d-b643bf4e2444&scope=openid&nonce=b80f6828-ea05-4122-937d-b643bf4e2444&registration=%7B%22client_name%22+%3A+%22Walt.id+Verifier+Portal%22%2C+%22client_purpose%22+%3A+%22Verification+of+0x2488fd38783d65e4fd46e7889eb113743334dbc772b05df382b8eadce763101b%22%2C+%22did_methods_supported%22+%3A+%5B%22did%3Aebsi%3A%22%5D%2C+%22logo_uri%22+%3A+null%2C+%22subject_identifier_types_supported%22+%3A+%5B%22did%22%5D%2C+%22tos_uri%22+%3A+null%2C+%22vp_formats%22+%3A+%7B%22jwt_vp%22+%3A+%7B%22alg%22+%3A+%5B%22EdDSA%22%2C+%22ES256K%22%5D%7D%2C+%22ldp_vp%22+%3A+%7B%22proof_type%22+%3A+%5B%22Ed25519Signature2018%22%5D%7D%7D%7D&exp=1644330535&iat=1644244135&claims=%7B%22vp_token%22+%3A+%7B%22presentation_definition%22+%3A+%7B%22id%22+%3A+%221%22%2C+%22input_descriptors%22+%3A+%5B%7B%22id%22+%3A+%221%22%2C+%22schema%22+%3A+%7B%22uri%22+%3A+%22https%3A%2F%2Fapi.preprod.ebsi.eu%2Ftrusted-schemas-registry%2Fv1%2Fschemas%2F0x2488fd38783d65e4fd46e7889eb113743334dbc772b05df382b8eadce763101b%22%7D%7D%5D%7D%7D%7D"
        val siopReq = OIDC4VPService(OIDCProvider("http://blank", "http://blank")).parseSIOPv2RequestUri(URI.create(reqUri))

        println(siopReq!!.redirect_uri)
        println(siopReq!!.claims.toJSONObject())
    }
}
