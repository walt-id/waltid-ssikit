package org.letstrust.services.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.letstrust.model.*
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals


class JsonSerializeEbsiTest {

    val format = Json { prettyPrint = true }

    private fun validateVC(fileName: String) {
        val expected = File("src/test/resources/ebsi/${fileName}").readText()
        // println(expected)
        val obj = Json.decodeFromString<VerifiableCredential>(expected)
        // println(obj)
        val encoded = Json.encodeToString(obj)
        // println(encoded)
        assertEquals(expected.replace("\\s".toRegex(), ""), Json.encodeToString(obj))
    }

//    @Test
//    fun ebsiDidTest() {
//        val expected = File("src/test/resources/dids/did-ebsi.json").readText()
//        println(expected)
//        val obj = Json.decodeFromString<DidEbsi>(expected)
//        println(obj)
//        val encoded = Json.encodeToString(obj)
//        println(encoded)
//        assertEquals(expected.replace("\\s".toRegex(), ""), Json.encodeToString(obj))
//    }

    @Test
    fun verifiableAuthorizationTest() {
        validateVC("verifiable-authorization.json")
    }

    @Test
    fun verifiableAttestationTest() {
        validateVC("verifiable-attestation.json")
    }

    @Test
    fun credentialStatusListTest() {
        val expected = File("src/test/resources/ebsi/verifiable-credential-status.json").readText()
        println(expected)
        val obj = Json.decodeFromString<List<CredentialStatusListEntry>>(expected)
        println(obj)
        assertEquals(expected.replace("\\s".toRegex(), ""), Json.encodeToString(obj))
    }

    @Test
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
            "EidasSeal2019",
            LocalDateTime.now().withNano(0),
            "did:creator",
            "assertionMethod",
            VerificationMethodCert("EidasCertificate2019", "1088321447"),
            "BD21J4fdlnBvBA+y6D...fnC8Y="
        )
        val serviceEndpoints = listOf(
            ServiceEndpoint(
                "did:example:123456789abcdefghi#agent",
                "AgentService",
                "https://agent.example.com/8377464"
            )
        )
        val eidasCertificate = EidasCertificate("123456", "123456", "blob")
        val issuer =
            Issuer("Brand Name", "www.domain.com", did, eidasCertificate, serviceEndpoints, organizationInfo, proof)
        val accreditationCredentials = listOf(
            VerifiableCredential(
                listOf(
                    "https://www.w3.org/2018/credentials/v1",
                    "https://essif.europa.eu/schemas/vc/2020/v1"
                ),
                "https://essif.europa.eu/tsr/53",
                listOf("VerifiableCredential", "VerifiableAttestation"),
                "did:ebsi:000098765",
                LocalDateTime.now().withNano(0),
                LocalDateTime.now().withNano(0),
                CredentialSubject("did:ebsi:00001235", null, null, listOf("claim1", "claim2")),
                CredentialStatus("https://essif.europa.eu/status/45", "CredentialsStatusList2020"),
                CredentialSchema("https://essif.europa.eu/tsr/education/CSR1224.json", "JsonSchemaValidator2018"),
                proof
            )
        )

        val tir = TrustedIssuerRegistry(issuer) // accreditationCredentials

        val string = format.encodeToString(tir)
        println(string)

        val obj = Json.decodeFromString<TrustedIssuerRegistry>(string)
        println(obj)

        assertEquals(tir, obj)
    }

    @Test
    fun trustedIssuerRegistryFileTest() {
        val expected = File("src/test/resources/ebsi/tir-organization-record.json").readText()
        val obj = Json.decodeFromString<TrustedIssuerRegistry>(expected)
        println(obj)
        val string = format.encodeToString(obj)
        println(string)
        assertEquals(expected.replace("\\s".toRegex(), ""), string.replace("\\s".toRegex(), ""))
    }

    @Test
    fun trustedAccreditationOrganizationFileTest() {
        val expected = File("src/test/resources/ebsi/taor-accreditation-organization-record.json").readText()
        val obj = Json.decodeFromString<TrustedAccreditationOrganizationRegistry>(expected)
        println(obj)
        val string = format.encodeToString(obj)
        println(string)
        assertEquals(expected.replace("\\s".toRegex(), ""), string.replace("\\s".toRegex(), ""))
    }

    @Test
    fun trustedSchemaRegistryFileTest() {
        val expected = File("src/test/resources/ebsi/trusted-schema-registry.json").readText()
        val obj = Json.decodeFromString<SchemaRegistry>(expected)
        println(obj)
        val string = format.encodeToString(obj)
        println(string)
        assertEquals(expected.replace("\\s".toRegex(), ""), string.replace("\\s".toRegex(), ""))
    }

    @Test
    fun trustedRevocationRegistryFileTest() {
        val expected = File("src/test/resources/ebsi/revocation-registry.json").readText()
        val obj = Json.decodeFromString<RevocationRegistry>(expected)
        println(obj)
        val string = format.encodeToString(obj)
        println(string)
        assertEquals(expected.replace("\\s".toRegex(), ""), string.replace("\\s".toRegex(), ""))
    }

    @Test
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
