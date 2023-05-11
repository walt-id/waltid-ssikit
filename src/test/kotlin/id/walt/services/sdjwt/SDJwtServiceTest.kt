package id.walt.services.sdjwt

import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.credentials.w3c.templates.VcTemplateService
import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.assertions.json.shouldNotMatchJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class SDJwtServiceTest: AnnotationSpec() {

    lateinit var keyId: String
    lateinit var sdJwtSvc: SDJwtService

    @BeforeAll
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        keyId = CryptoService.getService().generateKey(KeyAlgorithm.EdDSA_Ed25519).id
        sdJwtSvc = SDJwtService.getService()
    }

    private val jwtService = JwtService.getService()
    val testPayload = buildJsonObject {
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

    @Test
    fun testNestedSdPayloads() {
        val result_no_sds = sdJwtSvc.sign(keyId, testPayload, sdMap_no_sds)
        result_no_sds.sdPayload.keys shouldNotContain SDJwt.DIGESTS_KEY
        result_no_sds.sdPayload.keys shouldContainAll setOf("objectProp", "simpleProp")
        result_no_sds.disclosures shouldBe emptySet()
        sdJwtSvc.disclosePayload(result_no_sds).toString() shouldMatchJson testPayload.toString()
        sdJwtSvc.parseSDJwt(result_no_sds.toString()).toString() shouldBe result_no_sds.toString()

        val result_flat_obj =sdJwtSvc.sign(keyId, testPayload, sdMap_flat_obj)
        result_flat_obj.sdPayload.keys shouldNotContain "objectProp"
        result_flat_obj.sdPayload.keys shouldContainAll setOf("simpleProp", SDJwt.DIGESTS_KEY)
        result_flat_obj.disclosures shouldHaveSize 1
        sdJwtSvc.disclosePayload(result_flat_obj).toString() shouldMatchJson testPayload.toString()
        sdJwtSvc.parseSDJwt(result_flat_obj.toString()).toString() shouldBe result_flat_obj.toString()

        val result_flat_all_sd = sdJwtSvc.sign(keyId, testPayload, sdMap_flat_all_sd)
        result_flat_all_sd.sdPayload.keys shouldNotContainAnyOf setOf("objectProp", "simpleProp")
        result_flat_all_sd.sdPayload.keys shouldContain SDJwt.DIGESTS_KEY
        result_flat_all_sd.disclosures shouldHaveSize 2
        sdJwtSvc.disclosePayload(result_flat_all_sd).toString() shouldMatchJson testPayload.toString()

        val result_nested_flat = sdJwtSvc.sign(keyId, testPayload, sdMap_nested_flat)
        result_nested_flat.sdPayload.keys shouldNotContain "objectProp"
        result_nested_flat.sdPayload.keys shouldContainAll setOf(SDJwt.DIGESTS_KEY, "simpleProp")
        result_nested_flat.disclosures shouldHaveSize 2
        sdJwtSvc.disclosePayload(result_nested_flat).toString() shouldMatchJson testPayload.toString()

        val result_nested_nested = sdJwtSvc.sign(keyId, testPayload, sdMap_nested_nested)
        result_nested_nested.sdPayload.keys shouldNotContain "objectProp"
        result_nested_nested.sdPayload.keys shouldContainAll setOf(SDJwt.DIGESTS_KEY, "simpleProp")
        result_nested_nested.disclosures shouldHaveSize 3
        sdJwtSvc.disclosePayload(result_nested_nested).toString() shouldMatchJson testPayload.toString()

        val result_nested_mixed = sdJwtSvc.sign(keyId, testPayload, sdMap_nested_mixed)
        result_nested_mixed.sdPayload.keys shouldContainAll setOf("objectProp", "simpleProp")
        result_nested_mixed.sdPayload.keys shouldNotContainAnyOf setOf(SDJwt.DIGESTS_KEY)
        result_nested_mixed.sdPayload["objectProp"]!!.jsonObject.keys shouldNotContain "nestedProp1"
        result_nested_mixed.sdPayload["objectProp"]!!.jsonObject.keys shouldContainAll setOf(
            "nestedProp2",
            "nestedObj",
            SDJwt.DIGESTS_KEY
        )
        result_nested_mixed.sdPayload["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldNotContain "nestedObjProp1"
        result_nested_mixed.sdPayload["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldContain SDJwt.DIGESTS_KEY
        result_nested_mixed.disclosures shouldHaveSize 2
        sdJwtSvc.disclosePayload(result_nested_mixed).toString() shouldMatchJson testPayload.toString()
    }

    @Test
    fun testPresentNestedObject()
    {
        val sd_jwt_nested_nested = sdJwtSvc.sign(keyId, testPayload, sdMap_nested_nested)

        // Test nested disclosure selection
        val presentedJwt = SDJwtService.getService().present(sd_jwt_nested_nested, mapOf(
            "objectProp" to SDField(true, nestedMap = mapOf(
                "nestedObj" to SDField(true, nestedMap = mapOf(
                    "nestedObjProp1" to SDField(false)
                ))
            ))
        ))
        presentedJwt.digests2Disclosures.values.map { it.key } shouldContainAll setOf("objectProp", "nestedObj")
        presentedJwt.digests2Disclosures.values.map { it.key } shouldNotContain "nestedObjProp1"
        val presentedPayloadDisclosed = SDJwtService.getService().disclosePayload(presentedJwt)
        presentedPayloadDisclosed.keys shouldContainAll setOf("objectProp", "simpleProp")
        presentedPayloadDisclosed["objectProp"]!!.jsonObject.keys shouldContainAll setOf("nestedProp1", "nestedProp2", "nestedObj")
        presentedPayloadDisclosed["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldNotContain "nestedObjProp1"
        presentedPayloadDisclosed.toString() shouldNotMatchJson testPayload.toString()
    }

    @Test
    fun testParseFromString() {
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

    private fun filterSDMap(sdMap: Map<String, SDField>): Map<String,SDField> {
        return sdMap.filter { entry ->
            entry.value.sd || entry.value.nestedMap != null
        }.mapValues { entry ->
            if(entry.value.nestedMap != null) {
                SDField(entry.value.sd, filterSDMap(entry.value.nestedMap!!))
            } else entry.value
        }
    }
    @Test
    fun testGenerateSdMap() {
        val sd_jwt_nested_nested = sdJwtSvc.sign(keyId, testPayload, sdMap_nested_nested)
        val sdMap = sdJwtSvc.toSDMap(sd_jwt_nested_nested)
        // filter non-sd and non-nesting fields
        filterSDMap(sdMap) shouldBe sdMap_nested_nested
    }

    @Test
    fun testSelectDisclosures() {
        val sd_in_nested_only = mapOf(
            "objectProp" to SDField(false, nestedMap = mapOf(
                "nestedProp1" to SDField(true),
                "nestedObj" to SDField(false, nestedMap = mapOf(
                    "nestedObjProp1" to SDField(true)
                ))
            ))
        )
        val jwt_sd_in_nested_only = sdJwtSvc.sign(keyId, testPayload, sd_in_nested_only)
        jwt_sd_in_nested_only.disclosures shouldHaveSize 2
        val presented_sd_in_nested_only = sdJwtSvc.present(jwt_sd_in_nested_only, mapOf(
            "objectProp" to SDField(true, nestedMap = mapOf(
                "nestedObj" to SDField(true, nestedMap = mapOf(
                    "nestedObjProp1" to SDField(true)
                ))
            ))
        ))
        presented_sd_in_nested_only.disclosures shouldHaveSize 1
        val presentedDisclosedPayload = sdJwtSvc.disclosePayload(presented_sd_in_nested_only)
        presentedDisclosedPayload.keys shouldContain "objectProp"
        presentedDisclosedPayload["objectProp"]!!.jsonObject.keys shouldNotContain "nestedProp1"
        presentedDisclosedPayload["objectProp"]!!.jsonObject.keys shouldContain "nestedObj"
        presentedDisclosedPayload["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldContain "nestedObjProp1"

        //test select all by setting sdMap null
        val presented_all = sdJwtSvc.present(jwt_sd_in_nested_only, null)
        presented_all.disclosures shouldHaveSize 2
        val presentedAllDisclosedPayload = sdJwtSvc.disclosePayload(presented_all)
        presentedAllDisclosedPayload.keys shouldContain "objectProp"
        presentedAllDisclosedPayload["objectProp"]!!.jsonObject.keys shouldContain "nestedProp1"
        presentedAllDisclosedPayload["objectProp"]!!.jsonObject.keys shouldContain "nestedObj"
        presentedAllDisclosedPayload["objectProp"]!!.jsonObject["nestedObj"]!!.jsonObject.keys shouldContain "nestedObjProp1"
    }
}
