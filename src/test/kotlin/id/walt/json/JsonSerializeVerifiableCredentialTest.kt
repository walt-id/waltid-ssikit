package id.walt.services.data

import com.beust.klaxon.Klaxon
import id.walt.vclib.model.toCredential
import id.walt.vclib.credentials.Europass
import id.walt.vclib.credentials.PermanentResidentCard
import id.walt.vclib.credentials.VerifiableAttestation
import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.model.CredentialStatus
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import io.kotest.assertions.fail
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.time.LocalDateTime


class JsonSerializeVerifiableCredentialTest : AnnotationSpec() {

    val VC_PATH = "src/test/resources/verifiable-credentials"

    val format = Klaxon()

    @Test
    fun vcTemplatesTest() {
        File("templates/").walkTopDown()
            .filter { it.toString().endsWith(".json") }
            .forEach {
                println("serializing: $it")

                val input = File(it.toURI()).readText().replace("\\s".toRegex(), "")

                val vc = input.toCredential()

                when (vc) {
                    is Europass -> println("\t => Europass serialized")
                    is PermanentResidentCard -> {
                        println("\t => PermanentResidentCard serialized")
                        val enc = Klaxon().toJsonString(vc)
                        input shouldEqualJson enc
                    }
                    is VerifiableAttestation -> {
                        println("\t => EbsiVerifiableAttestation serialized")
                        val enc = Klaxon().toJsonString(vc)
                        input shouldEqualJson enc
                    }
                    else -> {
                        fail("VC type not supported")
                    }
                }
            }
    }

    @Test
    fun serializeEbsiVerifiableAuthorization() {
        val va = File("$VC_PATH/vc-ebsi-verifiable-authorisation.json").readText()
        val vc = va.toCredential()
    }

    @Test
    fun serializeSignedVc() {
        val signedEuropassStr = File("$VC_PATH/vc-europass-signed.json").readText()
        println(signedEuropassStr)
        val vc = signedEuropassStr.toCredential()
    }


    // TODO: remove / replace functions below as they are using the old data model

    @Test
    fun vcSerialization() {
        val input = File("templates/vc-template-default.json").readText().replace("\\s".toRegex(), "")
        val vc = Klaxon().parse<VerifiableCredential>(input)
        println(vc)
        val enc = Klaxon().toJsonString(vc)
        println(enc)
        input shouldEqualJson enc
    }

    @Test
    fun vcConstructTest() {

        val proof =
            Proof(
                type = "EidasSeal2019",
                created = LocalDateTime.now().withNano(0).toString(),
                creator = "did:creator",
                proofPurpose = "assertionMethod",
                verificationMethod = "EidasCertificate2019",//VerificationMethodCert("EidasCertificate2019", "1088321447"),
                jws = "BD21J4fdlnBvBA+y6D...fnC8Y="
            )
        val vc = VerifiableAttestation(
            context = listOf(
                "https://www.w3.org/2018/credentials/v1",
                "https://essif.europa.eu/schemas/v-a/2020/v1",
                "https://essif.europa.eu/schemas/eidas/2020/v1"
            ),
            id = "education#higherEducation#3fea53a4-0432-4910-ac9c-69ah8da3c37f",
            issuer = "did:ebsi:2757945549477fc571663bee12042873fe555b674bd294a3",
            issuanceDate = "2019-06-22T14:11:44Z",
            validFrom = "2019-06-22T14:11:44Z",
            credentialSubject = VerifiableAttestation.VerifiableAttestationSubject(
                id = "id123"
            ),
            credentialStatus = CredentialStatus(
                id = "https://essif.europa.eu/status/identity#verifiableID#1dee355d-0432-4910-ac9c-70d89e8d674e",
                type = "CredentialStatusList2020"
            ),
            credentialSchema = CredentialSchema(
                id = "https://essif.europa.eu/tsr-vid/verifiableid1.json",
                type = "JsonSchemaValidator2018"
            ),
            evidence = listOf(
                VerifiableAttestation.Evidence(
                    id = "https://essif.europa.eu/tsr-va/evidence/f2aeec97-fc0d-42bf-8ca7-0548192d5678",
                    type = listOf("DocumentVerification"),
                    verifier = "did:ebsi:2962fb784df61baa267c8132497539f8c674b37c1244a7a",
                    evidenceDocument = "Passport",
                    subjectPresence = "Physical",
                    documentPresence = "Physical"
                )
            ),
            proof = Proof(
                type = "EidasSeal2021",
                created = "2019-06-22T14:11:44Z",
                proofPurpose = "assertionMethod",
                verificationMethod = "did:ebsi:2757945549477fc571663bee12042873fe555b674bd294a3#2368332668",
                jws = "HG21J4fdlnBvBA+y6D...amP7O="
            )
        )


        val encoded = format.toJsonString(vc)
        // println(encoded)
        val obj = Klaxon().parse<VerifiableCredential>(encoded)
        // println(obj)

        vc shouldBe obj
    }

    @Test
    fun vcTemplatesOldTest() {
        File("templates/").walkTopDown()
            .filter { it.toString().endsWith(".json") }
            .forEach {
                println("serializing: $it")
                //val obj = Json { ignoreUnknownKeys = true }.decodeFromString<VerifiableCredential>(it.readText())
                val obj = Klaxon().parse<VerifiableCredential>(it.readText())
                println(obj)
            }
    }

}
