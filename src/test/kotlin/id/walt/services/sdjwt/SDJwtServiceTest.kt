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
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

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

        val disclosures_no_sds = mutableMapOf<String, SDisclosure>()
        val result_no_sds = WaltIdSDJwtService().generateSDPayload(payload, sdMap_no_sds, disclosures_no_sds)
        result_no_sds.keys shouldNotContain SDJwt.DIGESTS_KEY
        result_no_sds.keys shouldContainAll setOf("objectProp", "simpleProp")
        disclosures_no_sds shouldBe emptyMap()

        val disclosures_flat_obj = mutableMapOf<String, SDisclosure>()
        val result_flat_obj = WaltIdSDJwtService().generateSDPayload(payload, sdMap_flat_obj, disclosures_flat_obj)
        result_flat_obj.keys shouldNotContain "objectProp"
        result_flat_obj.keys shouldContainAll  setOf("simpleProp", SDJwt.DIGESTS_KEY)
        disclosures_flat_obj shouldHaveSize 1
        // TODO: parse disclosure to nested obj

        val disclosures_flat_all_sd = mutableMapOf<String, SDisclosure>()
        val result_flat_all_sd = WaltIdSDJwtService().generateSDPayload(payload, sdMap_flat_all_sd, disclosures_flat_all_sd)
        result_flat_all_sd.keys shouldNotContainAnyOf setOf("objectProp", "simpleProp")
        result_flat_all_sd.keys shouldContain SDJwt.DIGESTS_KEY
        disclosures_flat_all_sd shouldHaveSize 2
        // TODO: parse disclosure to nested obj

        val disclosures_nested_flat = mutableMapOf<String, SDisclosure>()
        val result_nested_flat = WaltIdSDJwtService().generateSDPayload(payload, sdMap_nested_flat, disclosures_nested_flat)
        result_nested_flat.keys shouldNotContain "objectProp"
        result_nested_flat.keys shouldContainAll setOf(SDJwt.DIGESTS_KEY, "simpleProp")
        disclosures_nested_flat shouldHaveSize 2
        // TODO: parse disclosures to nested objs

        val disclosures_nested_nested = mutableMapOf<String, SDisclosure>()
        val result_nested_nested = WaltIdSDJwtService().generateSDPayload(payload, sdMap_nested_nested, disclosures_nested_nested)
        result_nested_nested.keys shouldNotContain "objectProp"
        result_nested_nested.keys shouldContainAll setOf(SDJwt.DIGESTS_KEY, "simpleProp")
        disclosures_nested_nested shouldHaveSize 3
        // TODO: parse disclosures to nested objs

        val disclosures_nested_mixed = mutableMapOf<String, SDisclosure>()
        val result_nested_mixed = WaltIdSDJwtService().generateSDPayload(payload, sdMap_nested_mixed, disclosures_nested_mixed)
        result_nested_mixed.keys shouldContainAll setOf("objectProp", "simpleProp")
        result_nested_mixed.keys shouldNotContainAnyOf setOf(SDJwt.DIGESTS_KEY)
        result_nested_mixed["objectProp"]!!.jsonObject.keys shouldNotContain "nestedProp1"
        result_nested_mixed["objectProp"]!!.jsonObject.keys shouldContainAll setOf("nestedProp2", "nestedObj", SDJwt.DIGESTS_KEY)
        result_nested_mixed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldNotContain "nestedObjProp1"
        result_nested_mixed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldContain SDJwt.DIGESTS_KEY
        disclosures_nested_mixed shouldHaveSize 2
        // TODO: parse disclosures to nested objs

        // Test signing and parsing of nested objects
        val keyId = cryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        val sd_jwt_nested_nested = SDJwtService.getService().sign(keyId.id, payload, sdMap_nested_nested)
        val parsedUndisclosed_nested_nested = sd_jwt_nested_nested.sdPayload
        val parsedDisclosed_nested_nested = SDJwtService.getService().disclosePayload(sd_jwt_nested_nested)
        parsedUndisclosed_nested_nested.keys shouldNotContain "objectProp"
        parsedUndisclosed_nested_nested.keys shouldContainAll setOf(SDJwt.DIGESTS_KEY, "simpleProp")
        parsedDisclosed_nested_nested.keys shouldNotContain SDJwt.DIGESTS_KEY
        parsedDisclosed_nested_nested.keys shouldContainAll setOf("objectProp", "simpleProp")
        parsedDisclosed_nested_nested["objectProp"]!!.jsonObject.keys shouldContain "nestedObj"
        parsedDisclosed_nested_nested["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldContain "nestedObjProp1"
        parsedDisclosed_nested_nested["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject["nestedObjProp1"]!!.jsonPrimitive.int shouldBe 1234

        val sd_jwt_nested_mixed = SDJwtService.getService().sign(keyId.id, payload, sdMap_nested_mixed)
        val parsedUndisclosed_nested_mixed = sd_jwt_nested_mixed.sdPayload
        val parsedDisclosed_nested_mixed = SDJwtService.getService().disclosePayload(sd_jwt_nested_mixed)
        parsedUndisclosed_nested_mixed.keys shouldContainAll setOf("objectProp", "simpleProp")
        parsedUndisclosed_nested_mixed.keys shouldNotContain SDJwt.DIGESTS_KEY
        parsedUndisclosed_nested_mixed["objectProp"]!!.jsonObject.keys shouldContainAll setOf("nestedObj", "nestedProp2", SDJwt.DIGESTS_KEY)
        parsedUndisclosed_nested_mixed["objectProp"]!!.jsonObject.keys shouldNotContain "nestedProp1"
        parsedUndisclosed_nested_mixed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldContain SDJwt.DIGESTS_KEY
        parsedUndisclosed_nested_mixed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldNotContain "nestedObjProp1"
        parsedDisclosed_nested_mixed["objectProp"]!!.jsonObject.keys shouldContain "nestedProp1"
        parsedDisclosed_nested_mixed["objectProp"]!!.jsonObject.keys shouldNotContain SDJwt.DIGESTS_KEY
        parsedDisclosed_nested_mixed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldContain "nestedObjProp1"
        parsedDisclosed_nested_mixed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldNotContain SDJwt.DIGESTS_KEY

        // Test nested disclosure selection
        val presentedJwt = SDJwtService.getService().present(sd_jwt_nested_nested, mapOf(
            "objectProp" to SDField(true, nestedMap = mapOf(
                "nestedObj" to SDField(true, nestedMap = mapOf(
                    "nestedObjProp1" to SDField(false)
                ))
            ))
        ))
        presentedJwt.disclosures.map { it.key } shouldContainAll setOf("objectProp", "nestedObj")
        presentedJwt.disclosures.map { it.key } shouldNotContain "nestedObjProp1"
        val presentedPayloadDisclosed = SDJwtService.getService().disclosePayload(presentedJwt)
        presentedPayloadDisclosed.keys shouldContainAll setOf("objectProp", "simpleProp")
        presentedPayloadDisclosed["objectProp"]!!.jsonObject.keys shouldContainAll setOf("nestedProp1", "nestedProp2", "nestedObj")
        presentedPayloadDisclosed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldNotContain "nestedObjProp1"
    }

    @Test
    fun testParsing() {
        val combinedSdJwt = SDJwtService.getService().parseSDJwt("eyJraWQiOiIyNDlmOWMzZTRkMGQ0ZGU4YTAzMDkyNmE3ZmExZmU4YiIsInR5cCI6IkpXVCIsImFsZyI6IkVkRFNBIn0.eyJjcmVkZW50aWFsU2NoZW1hIjp7ImlkIjoiaHR0cHM6Ly9yYXcuZ2l0aHVidXNlcmNvbnRlbnQuY29tL3dhbHQtaWQvd2FsdGlkLXNzaWtpdC12Y2xpYi9tYXN0ZXIvc3JjL3Rlc3QvcmVzb3VyY2VzL3NjaGVtYXMvVmVyaWZpYWJsZUlkLmpzb24iLCJ0eXBlIjoiRnVsbEpzb25TY2hlbWFWYWxpZGF0b3IyMDIxIn0sImV2aWRlbmNlIjpbeyJkb2N1bWVudFByZXNlbmNlIjpbIlBoeXNpY2FsIl0sImV2aWRlbmNlRG9jdW1lbnQiOlsiUGFzc3BvcnQiXSwic3ViamVjdFByZXNlbmNlIjoiUGh5c2ljYWwiLCJ0eXBlIjpbIkRvY3VtZW50VmVyaWZpY2F0aW9uIl0sInZlcmlmaWVyIjoiZGlkOmVic2k6MkE5Qlo5U1VlNkJhdGFjU3B2czFWNUNkakh2THBRN2JFc2kySmI2TGRIS25ReGFOIn1dLCJpc3N1YW5jZURhdGUiOiIyMDIxLTA4LTMxVDAwOjAwOjAwWiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImlkIjoiZGlkOmVic2k6MkFFTUFxWFdLWU11MUpIUEFnR2NnYTRkeHU3VGhnZmdOOTVWeUpCSkdaYlNKVXRwIiwiY3VycmVudEFkZHJlc3MiOlsiMSBCb3VsZXZhcmQgZGUgbGEgTGliZXJ0w6ksIDU5ODAwIExpbGxlIl0sImZhbWlseU5hbWUiOiJET0UiLCJmaXJzdE5hbWUiOiJKYW5lIiwiZ2VuZGVyIjoiRkVNQUxFIiwibmFtZUFuZEZhbWlseU5hbWVBdEJpcnRoIjoiSmFuZSBET0UiLCJwZXJzb25hbElkZW50aWZpZXIiOiIwOTA0MDA4MDg0SCIsInBsYWNlT2ZCaXJ0aCI6IkxJTExFLCBGUkFOQ0UiLCJfc2QiOlsiUmQtOFpuZFQwb1M3RVZ5QUI4S1o5dVA5WDdjeXRrUGhkZ2ZHbWZRMmxiYyJdfSwiaWQiOiJ1cm46dXVpZDozYWRkOTRmNC0yOGVjLTQyYTEtODcwNC00ZTRhYTUxMDA2YjQiLCJ2YWxpZEZyb20iOiIyMDIxLTA4LTMxVDAwOjAwOjAwWiIsInR5cGUiOlsiVmVyaWZpYWJsZUNyZWRlbnRpYWwiLCJWZXJpZmlhYmxlQXR0ZXN0YXRpb24iLCJWZXJpZmlhYmxlSWQiXSwiaXNzdWVkIjoiMjAyMS0wOC0zMVQwMDowMDowMFoiLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJpc3N1ZXIiOiJkaWQ6ZWJzaToyQTlCWjlTVWU2QmF0YWNTcHZzMVY1Q2RqSHZMcFE3YkVzaTJKYjZMZEhLblF4YU4ifQ.wY28K6qJ0R0niY5xHaOAlTpO56EDEziClXwY1Nz5nCfacbm6xte2s_PfnM5XI1fFQ3Mv-EHlk824Tp4as835Ag~WyIxQkh5R0o5Ym5Cb3ZDQ0puRmRVZUpRIiwiZGF0ZU9mQmlydGgiLCIxOTkzLTA0LTA4Il0~eqwe.saddsf.dsgfdfg")

        val payloadUndisclosed = combinedSdJwt.sdPayload
        val payloadResolved = SDJwtService.getService().disclosePayload(combinedSdJwt)
        payloadUndisclosed.keys shouldContain "credentialSubject"
        payloadUndisclosed["credentialSubject"]!!.jsonObject.keys shouldContain SDJwt.DIGESTS_KEY
        payloadUndisclosed["credentialSubject"]!!.jsonObject.keys shouldNotContain "dateOfBirth"
        payloadResolved.keys shouldContain "credentialSubject"
        payloadResolved["credentialSubject"]!!.jsonObject.keys shouldContain "dateOfBirth"
        payloadResolved["credentialSubject"]!!.jsonObject.keys shouldNotContain SDJwt.DIGESTS_KEY
    }
}
