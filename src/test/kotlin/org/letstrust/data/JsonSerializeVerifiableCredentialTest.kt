package org.letstrust.services.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.letstrust.model.*
import java.io.File
import java.time.LocalDateTime
import kotlin.test.assertEquals


class JsonSerializeVerifiableCredentialTest {

    val format = Json { prettyPrint = true }

    @Test
    fun serializeExample37() {
        val expected = File("src/test/resources/verifiable-credentials/vc-example37.json").readText()
        // println(expected)
        val obj = Json.decodeFromString<VerifiableCredentialPRC>(expected)
        // println(obj)
        val encoded = Json.encodeToString(obj)
        // println(encoded)
        assertEquals(expected.replace("\\s".toRegex(), ""), Json.encodeToString(obj).replace("\\s".toRegex(), ""))
    }

    @Test
    fun vcConstructTest() {

        val proof =
            Proof(
                "EidasSeal2019",
                LocalDateTime.now().withNano(0),
                "did:creator",
                "assertionMethod",
                VerificationMethodCert("EidasCertificate2019", "1088321447"),
                "BD21J4fdlnBvBA+y6D...fnC8Y="
            )
        val vc = VerifiableCredential(
            listOf(
                "https://www.w3.org/2018/credentials/v1",
                "https://essif.europa.eu/schemas/vc/2020/v1"
            ),
            "https://essif.europa.eu/tsr/53",
            listOf("VerifiableCredential", "VerifiableAttestation"),
            "did:ebsi:000098765",
            LocalDateTime.now().withNano(0),
            null,
            CredentialSubject("did:ebsi:00001235", null, null, listOf("claim1", "claim2")),
            CredentialStatus("https://essif.europa.eu/status/45", "CredentialsStatusList2020"),
            CredentialSchema("https://essif.europa.eu/tsr/education/CSR1224.json", "JsonSchemaValidator2018"),
            proof
        )


        val encoded = format.encodeToString(vc)
        // println(encoded)
        val obj = Json.decodeFromString<VerifiableCredential>(encoded)
        // println(obj)

        assertEquals(vc, obj)
    }

    @Test
    fun vcTemplatesTest() {
        File("templates/").walkTopDown()
            .filter { it.toString().endsWith(".json") }
            .forEach {
                println("serializing: $it")
                val obj = Json { ignoreUnknownKeys = true }.decodeFromString<VerifiableCredential>(it.readText())
                println(obj)
            }
    }

    //TODO not all files working yet @Test
    fun vcExamplesTest() {
        File("src/test/resources/verifiable-credentials/").walkTopDown()
            .filter { it.toString().endsWith(".json") }
            .forEach {
                println("serializing: $it")
            }
    }
}