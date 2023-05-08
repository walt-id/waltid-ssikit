package id.walt.services.sdjwt

import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.credentials.w3c.templates.VcTemplateService
import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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
            "credentialSubject" to SDField(sd = false, nestedMap = mapOf(
                "dateOfBirth" to SDField(sd = true)
            ))
        )

        val keyId = cryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        println(
            SDJwtService.getService().sign(keyId.id, payload, sdMap)
        )
    }

    @Test
    fun testNestedSDClaims() {
        val payload = buildJsonObject {
            put("objectProp", buildJsonObject {
                put("nestedProp1", "value1")
                put("nestedProp2", "value2")
                put("nestedObj", buildJsonObject {
                    put("nestedObjProp1", 1234)
                })
            })
            put("simpleProp", true)
        }
        val sdMap_no_sds = mapOf<String, SDField>()
        val sdMap_flat_obj = mapOf(
            "objectProp" to SDField(true)
        )
        val sdMap_flat_all_sd = mapOf(
            "objectProp" to SDField(true),
            "simpleProp" to SDField(true)
        )
        val sdMap_nested_flat = mapOf(
            "objectProp" to SDField(true, nestedMap = mapOf(
                "nestedObj" to SDField(true)
            ))
        )
        val sdMap_nested_nested = mapOf(
            "objectProp" to SDField(true, nestedMap = mapOf(
                "nestedObj" to SDField(true, nestedMap = mapOf(
                    "nestedObjProp1" to SDField(true)
                ))
            ))
        )
        val sdMap_nested_mixed = mapOf(
            "objectProp" to SDField(false, nestedMap = mapOf(
                "nestedProp1" to SDField(true),
                "nestedObj" to SDField(false, nestedMap = mapOf(
                    "nestedObjProp1" to SDField(true)
                ))
            ))
        )

        val disclosures_no_sds = mutableSetOf<String>()
        val result_no_sds = WaltIdSDJwtService().generateSDPayload(payload, sdMap_no_sds, disclosures_no_sds)
        result_no_sds.keys shouldNotContain WaltIdSDJwtService.DIGESTS_KEY
        result_no_sds.keys shouldContainAll setOf("objectProp", "simpleProp")
        disclosures_no_sds shouldBe emptySet()

        val disclosures_flat_obj = mutableSetOf<String>()
        val result_flat_obj = WaltIdSDJwtService().generateSDPayload(payload, sdMap_flat_obj, disclosures_flat_obj)
        result_flat_obj.keys shouldNotContain "objectProp"
        result_flat_obj.keys shouldContainAll  setOf("simpleProp", WaltIdSDJwtService.DIGESTS_KEY)
        disclosures_flat_obj shouldHaveSize 1
        // TODO: parse disclosure to nested obj

        val disclosures_flat_all_sd = mutableSetOf<String>()
        val result_flat_all_sd = WaltIdSDJwtService().generateSDPayload(payload, sdMap_flat_all_sd, disclosures_flat_all_sd)
        result_flat_all_sd.keys shouldNotContainAnyOf setOf("objectProp", "simpleProp")
        result_flat_all_sd.keys shouldContain WaltIdSDJwtService.DIGESTS_KEY
        disclosures_flat_all_sd shouldHaveSize 2
        // TODO: parse disclosure to nested obj

        val disclosures_nested_flat = mutableSetOf<String>()
        val result_nested_flat = WaltIdSDJwtService().generateSDPayload(payload, sdMap_nested_flat, disclosures_nested_flat)
        result_nested_flat.keys shouldNotContain "objectProp"
        result_nested_flat.keys shouldContainAll setOf(WaltIdSDJwtService.DIGESTS_KEY, "simpleProp")
        disclosures_nested_flat shouldHaveSize 2
        // TODO: parse disclosures to nested objs

        val disclosures_nested_nested = mutableSetOf<String>()
        val result_nested_nested = WaltIdSDJwtService().generateSDPayload(payload, sdMap_nested_nested, disclosures_nested_nested)
        result_nested_nested.keys shouldNotContain "objectProp"
        result_nested_nested.keys shouldContainAll setOf(WaltIdSDJwtService.DIGESTS_KEY, "simpleProp")
        disclosures_nested_nested shouldHaveSize 3
        // TODO: parse disclosures to nested objs

        val disclosures_nested_mixed = mutableSetOf<String>()
        val result_nested_mixed = WaltIdSDJwtService().generateSDPayload(payload, sdMap_nested_mixed, disclosures_nested_mixed)
        result_nested_mixed.keys shouldContainAll setOf("objectProp", "simpleProp")
        result_nested_mixed.keys shouldNotContainAnyOf setOf(WaltIdSDJwtService.DIGESTS_KEY)
        result_nested_mixed["objectProp"]!!.jsonObject.keys shouldNotContain "nestedProp1"
        result_nested_mixed["objectProp"]!!.jsonObject.keys shouldContainAll setOf("nestedProp2", "nestedObj", WaltIdSDJwtService.DIGESTS_KEY)
        result_nested_mixed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldNotContain "nestedObjProp1"
        result_nested_mixed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldContain WaltIdSDJwtService.DIGESTS_KEY
        disclosures_nested_mixed shouldHaveSize 2
        // TODO: parse disclosures to nested objs
    }
}
