package org.letstrust.services.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.letstrust.model.DidEbsi
import org.letstrust.model.DidWeb
import org.letstrust.test.readDid
import kotlin.test.assertEquals


class JsonSerializeDidsTest {

    val format = Json { prettyPrint = true; ignoreUnknownKeys = true }


    fun serializeDidWeb(didWeb: String) {

        val obj = Json.decodeFromString<DidWeb>(didWeb)
        // println(obj)
        val encoded = format.encodeToString(obj)
        // println(encoded)
        assertEquals(didWeb.replace("\\s".toRegex(), ""), encoded.replace("\\s".toRegex(), ""))
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

        val encoded = format.encodeToString(didWeb)
        // println(encoded)
        val obj = Json.decodeFromString<DidWeb>(encoded)
        // println(obj)

        assertEquals(didWeb, obj)
    }

    @Test
    fun serializeUniResDidWeb() {
        serializeDidWeb(readDid("web/did-web-unires"))
    }

    @Test
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

        val obj = Json.decodeFromString<DidEbsi>(didEbsi)
        // println(obj)
        val encoded = format.encodeToString(obj)
        // println(encoded)
        assertEquals(didEbsi.replace("\\s".toRegex(), ""), encoded.replace("\\s".toRegex(), ""))
    }

    // TODO: NOT WORKING (creator not supported) @Test
    fun serializeDidEbsiLT() {
        val didEbsi = readDid("did-ebsi-lt")

        val obj = Json.decodeFromString<DidEbsi>(didEbsi)
        // println(obj)
        val encoded = format.encodeToString(obj)
        // println(encoded)
        assertEquals(didEbsi.replace("\\s".toRegex(), ""), encoded.replace("\\s".toRegex(), ""))
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
