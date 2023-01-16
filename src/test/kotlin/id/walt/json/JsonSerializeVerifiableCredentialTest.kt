package id.walt.json

import com.beust.klaxon.Klaxon
import id.walt.credentials.w3c.toVerifiableCredential
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.AnnotationSpec
import java.io.File


class JsonSerializeVerifiableCredentialTest : AnnotationSpec() {

    val VC_PATH = "src/test/resources/verifiable-credentials"

    val format = Klaxon()

    @Test
    fun vcTemplatesTest() {
        File("$VC_PATH/").walkTopDown()
            .filter { it.toString().endsWith(".json") }
            .forEach {
                println("serializing: $it")

                val input = File(it.toURI()).readText().replace("\\s".toRegex(), "")
                input.toVerifiableCredential().toJson() shouldMatchJson input
            }
    }

    @Test
    fun serializeEbsiVerifiableAuthorization() {
        val va = File("$VC_PATH/vc-ebsi-verifiable-authorisation.json").readText()
        val vc = va.toVerifiableCredential()
    }

    @Test
    fun serializeSignedVc() {
        val signedEuropassStr = File("$VC_PATH/vc-europass-signed.json").readText()
        println(signedEuropassStr)
        val vc = signedEuropassStr.toVerifiableCredential()
    }


    // TODO: remove / replace functions below as they are using the old data model

    @Test
    fun vcSerialization() {
        val input = File("$VC_PATH/vc-template-default.json").readText().replace("\\s".toRegex(), "")
        val vc = input.toVerifiableCredential()
        println(vc)
        val enc = vc.toJson()
        println(enc)
        input shouldEqualJson enc
    }

}
