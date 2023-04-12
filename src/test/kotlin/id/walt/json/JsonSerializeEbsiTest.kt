package id.walt.json

import com.beust.klaxon.Klaxon
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.model.*
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class JsonSerializeEbsiTest : AnnotationSpec() {

    val format = Klaxon()

    private fun validateVC(fileName: String) {
        val expected = File("src/test/resources/ebsi/${fileName}").readText()
        // println(expected)
        val obj = expected.toVerifiableCredential()
        // println(obj)
        val encoded = obj.toJson()
        // println(encoded)
        expected shouldMatchJson encoded
    }

//    //@Test
//    fun ebsiDidTest() {
//        val expected = File("src/test/resources/dids/did-ebsi.json").readText()
//        println(expected)
//        val obj = Klaxon().parse<DidEbsi>(expected)
//        println(obj)
//        val encoded = Klaxon().toJsonString(obj)
//        println(encoded)
//        expected.replace("\\s".toRegex(), "") shouldBe Klaxon().toJsonString(obj))
//    }

    ////@Test
    fun verifiableAuthorizationTest() {
        validateVC("verifiable-authorization.json")
    }

    ////@Test
    fun verifiableAttestationTest() {
        validateVC("verifiable-attestation.json")
    }

    ////@Test
    fun credentialStatusListTest() {
        val expected = File("src/test/resources/ebsi/verifiable-vc-status-revoked.json").readText()
        println(expected)
        val obj = Klaxon().parse<List<CredentialStatusListEntry>>(expected)
        println(obj)
        expected.replace("\\s".toRegex(), "") shouldBe Klaxon().toJsonString(obj)
    }

    //@Test
    fun trustedIssuerRegistryFileTest() {
        val expected = File("src/test/resources/ebsi/tir-organization-record.json").readText()
        val obj = format.parse<TrustedIssuerRegistry>(expected)
        println(obj)
        val string = format.toJsonString(obj)
        println(string)
        expected.replace("\\s".toRegex(), "") shouldBe string.replace("\\s".toRegex(), "")
    }

    //@Test
    fun trustedAccreditationOrganizationFileTest() {
        val expected = File("src/test/resources/ebsi/taor-accreditation-organization-record.json").readText()
        val obj = format.parse<TrustedAccreditationOrganizationRegistry>(expected)
        println(obj)
        val string = format.toJsonString(obj)
        println(string)
        expected.replace("\\s".toRegex(), "") shouldBe string.replace("\\s".toRegex(), "")
    }

    //@Test
    fun trustedSchemaRegistryFileTest() {
        val expected = File("src/test/resources/ebsi/trusted-schema-registry.json").readText()
        val obj = format.parse<SchemaRegistry>(expected)
        println(obj)
        val string = format.toJsonString(obj)
        println(string)
        expected.replace("\\s".toRegex(), "") shouldBe string.replace("\\s".toRegex(), "")
    }

    //@Test
    fun trustedRevocationRegistryFileTest() {
        val expected = File("src/test/resources/ebsi/revocation-registry.json").readText()
        val obj = format.parse<RevocationRegistry>(expected)
        println(obj)
        val string = format.toJsonString(obj)
        println(string)
        expected.replace("\\s".toRegex(), "") shouldBe string.replace("\\s".toRegex(), "")
    }

    //@Test
    fun dateTest() {

        val inDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC)

        val inDateEpochSeconds = Instant.ofEpochSecond(inDateTime.toEpochSecond())

        val dateStr = DateTimeFormatter.ISO_INSTANT.format(inDateEpochSeconds)

        println("STRING:  $dateStr") // 2021-02-11T15:38:00Z

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val outDateTime = LocalDateTime.parse(dateStr, formatter)

        println("DATE TIME:  $outDateTime") // 2021-02-11T15:41:01

    }
}
