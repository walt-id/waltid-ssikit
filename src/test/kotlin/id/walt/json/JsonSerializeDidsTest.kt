package id.walt.json

import com.beust.klaxon.Klaxon
import id.walt.model.DidEbsi
import id.walt.model.DidWeb
import id.walt.test.readDid
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.AnnotationSpec


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
    fun serializeDidEbsi() {
        val didEbsi = readDid("did-ebsi")

        val obj = Klaxon().parse<DidEbsi>(didEbsi)
        // println(obj)
        val encoded = format.toJsonString(obj)
        // println(encoded)
        didEbsi shouldEqualJson encoded
    }

}
