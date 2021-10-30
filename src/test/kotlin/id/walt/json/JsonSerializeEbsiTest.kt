package id.walt.services.data

import com.beust.klaxon.Klaxon
import id.walt.model.*
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
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
        val obj = format.parse<VerifiableCredential>(expected)
        // println(obj)
        val encoded = format.toJsonString(obj)
        // println(encoded)
        expected.replace("\\s".toRegex(), "") shouldBe Klaxon().toJsonString(obj)
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
        val expected = File("src/test/resources/ebsi/verifiable-credential-status.json").readText()
        println(expected)
        val obj = Klaxon().parse<List<CredentialStatusListEntry>>(expected)
        println(obj)
        expected.replace("\\s".toRegex(), "") shouldBe Klaxon().toJsonString(obj)
    }

    ////@Test
    fun trustedIssuerRegistryObjTest() {

        val did = listOf("did:ebsi:00003333", "did:ebsi:00005555")
        val organizationInfo =
            OrganizationInfo(
                "123456789",
                "Example Legal Name",
                "Example Street 42, Vienna, Austria",
                "https://great.company.be",
                "123456789",
                "12341212EXAMPLE34512",
                "AT123456789101",
                "AT12345678910",
                "1234",
                "https://example.organization.com"
            )
        val proof = Proof(
            type = "EidasSeal2019",
            created = LocalDateTime.now().withNano(0).toString(),
            creator = "did:creator",
            proofPurpose = "assertionMethod",
            verificationMethod = "EidasCertificate2019",//VerificationMethodCert("EidasCertificate2019", "1088321447"),
            jws = "BD21J4fdlnBvBA+y6D...fnC8Y="
        )
        val serviceEndpoints = listOf(
            ServiceEndpoint(
                id = "did:example:123456789abcdefghi#agent",
                type = "AgentService",
                serviceEndpoint = "https://agent.example.com/8377464"
            )
        )
        val eidasCertificate = EidasCertificate("123456", "123456", "blob")
        val issuer =
            Issuer("Brand Name", "www.domain.com", did, eidasCertificate, serviceEndpoints, organizationInfo, proof)

        val tir = TrustedIssuerRegistry(issuer) // accreditationCredentials

        val string = format.toJsonString(tir)
        println(string)

        val obj = format.parse<TrustedIssuerRegistry>(string)
        println(obj)

        tir shouldBe obj
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
