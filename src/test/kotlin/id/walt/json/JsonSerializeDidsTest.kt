package id.walt.json

import com.beust.klaxon.Klaxon
import id.walt.model.DidEbsi
import id.walt.model.DidWeb
import id.walt.test.readDid
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe


class JsonSerializeDidsTest : AnnotationSpec() {

    val format = Klaxon()


    fun serializeDidWeb(didWeb: String) {

        val obj = Klaxon().parse<DidWeb>(didWeb)
        // println(obj)
        val encoded = format.toJsonString(obj)
        // println(encoded)
        didWeb shouldEqualJson encoded
    }

    @Test
    fun constructDidWebTest() {

        val keyRef = listOf("did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN")

        val pubKey = DidWeb.PublicKey(
            "did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN",
            "did:web:did.actor:alice",
            "Ed25519VerificationKey2018",
            "DK7uJiq9PnPnj7AmNZqVBFoLuwTjT1hFPrk6LSjZ2JRz"
        )

        val keyAgreement = DidWeb.KeyAgreement(
            "did:web:did.actor:alice#zC8GybikEfyNaausDA4mkT4egP7SNLx2T1d1kujLQbcP6h",
            "X25519KeyAgreementKey2019",
            "Ed25519VerificationKey2018",
            "CaSHXEvLKS6SfN9aBfkVGBpp15jSnaHazqHgLHp8KZ3Y"
        )

        val didWeb = DidWeb(
            "https://w3id.org/did/v0.11",
            "did:web:did.actor:alice",
            listOf(pubKey),
            listOf(keyAgreement),
            keyRef,
            keyRef,
            keyRef,
            keyRef
        )

        val encoded = format.toJsonString(didWeb)
        // println(encoded)
        val obj = Klaxon().parse<DidWeb>(encoded)
        // println(obj)

        didWeb shouldBe obj
    }

    // @Test
    fun serializeUniResDidWeb() {
        serializeDidWeb(readDid("web/did-web-unires"))
    }

    // @Test
    fun serializeMattrDidWeb() {
        serializeDidWeb(readDid("web/did-web-mattr"))
    }

    // @Test
    fun serializeTransumuteDidWeb() {
        serializeDidWeb(readDid("web/did-web-transmute"))
    }

    // @Test
    fun serializeExample1DidWeb() {
        serializeDidWeb(readDid("web/did-web-example1"))
    }

    @Test
    fun serializeDidEbsi() {
        val didEbsi = readDid("did-ebsi")

        val obj = Klaxon().parse<DidEbsi>(didEbsi)
        // println(obj)
        val encoded = format.toJsonString(obj)
        // println(encoded)
        didEbsi shouldEqualJson encoded
    }

    // TODO: NOT WORKING (creator not supported) @Test
    fun serializeDidEbsiLT() {
        val didEbsi = readDid("did-ebsi-lt")

        val obj = Klaxon().parse<DidEbsi>(didEbsi)
        // println(obj)
        val encoded = format.toJsonString(obj)
        // println(encoded)
        didEbsi.replace("\\s".toRegex(), "") shouldBe encoded.replace("\\s".toRegex(), "")
    }

    //TODO: NOT WORKING @Test
//    fun serializeAllDidWebExamples() {
//        File("src/test/resources/dids/web").walkTopDown()
//            .filter { it.toString().endsWith(".json") }
//            .forEach {
//                println("serializing: $it")
//                serializeDidWeb(it)
//            }
//    }
}
