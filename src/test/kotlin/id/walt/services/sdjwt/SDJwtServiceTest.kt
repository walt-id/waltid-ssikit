package id.walt.services.sdjwt

import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.templates.VcTemplateService
import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SDJwtServiceTest: AnnotationSpec() {


    @BeforeAll
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()
    private val jwtService = JwtService.getService()

    @Test
    fun testSignRawSdJwt() {
        val credential = VcTemplateService.getService().getTemplate("VerifiableId").template!!
        val payload = credential.toJsonObject()
        val sdMap = mapOf(
            "credentialSubject" to SDField(isSelectivelyDisclosable = false, isDisclosed = true, nestedMap = mapOf(
                "dateOfBirth" to SDField(isSelectivelyDisclosable = true, isDisclosed = false)
            ))
        )

        val keyId = cryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        println(
            SDJwtService.getService().sign(keyId.id, payload, sdMap)
        )
    }
}
