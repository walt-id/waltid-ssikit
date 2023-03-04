package id.walt.json

import com.beust.klaxon.Klaxon
import id.walt.model.Did
import id.walt.model.did.DidEbsi
import id.walt.model.did.DidWeb
import id.walt.test.readDid
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.AnnotationSpec


class JsonSerializeDidsTest : AnnotationSpec() {

    val format = Klaxon()

    fun serializeDidWeb(didJson: String) {

        val did = Klaxon().parse<DidWeb>(didJson)
        // println(obj)
        val encoded = format.toJsonString(did)
        // println(encoded)
        encoded shouldEqualJson didJson
    }

    @Test
    fun serializeExample1() {

        // Single value @context and service
        val didJson = readDid("did-example1")

        val did = Did.decode(didJson) as Did
        // println(obj)
        val encoded = did.encodePretty()
        // println(encoded)
        encoded shouldEqualJson didJson
    }

    @Test
    @Ignore
    fun serializeExample2() {

        /**
         * controller not supported
         * https://www.w3.org/TR/did-core/#did-controller
         */

        // Single value @context and service
        val didJson = readDid("did-example2")

        val did = Did.decode(didJson) as Did
        // println(obj)
        val encoded = did.encodePretty()
        // println(encoded)
        encoded shouldEqualJson didJson
    }

    @Test
    fun serializeDidEbsi() {
        val didJson = readDid("did-ebsi")

        val did = DidEbsi.decode(didJson) as DidEbsi
        // println(obj)
        val encoded = did.encodePretty()
        // println(encoded)
        encoded shouldEqualJson didJson
    }

    @Test
    fun serializeDidPeerNumalgo0() {
        val didJson = readDid("did-peer0")

        val did = Did.decode(didJson) as Did
        // println(obj)
        val encoded = did.encodePretty()
        // println(encoded)
        encoded shouldEqualJson didJson
    }

    @Test
    fun serializeDidPeerNumalgo2() {
        val didJson = readDid("did-peer2")

        val did = Did.decode(didJson) as Did
        // println(obj)
        val encoded = did.encodePretty()
        // println(encoded)
        encoded shouldEqualJson didJson
    }
}
