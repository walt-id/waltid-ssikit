package id.walt.rest

import id.walt.auditor.Auditor
import id.walt.auditor.SignaturePolicy
import id.walt.common.readWhenContent
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.model.DidMethod
import id.walt.rest.custodian.CustodianAPI
import id.walt.rest.custodian.ExportKeyRequest
import id.walt.rest.custodian.PresentCredentialsRequest
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.key.KeyFormat
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.toCredential
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import java.io.File

class CustodianApiTest : StringSpec({

    ServiceMatrix("service-matrix.properties")

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    println("${CustodianAPI.DEFAULT_BIND_ADDRESS}/${CustodianAPI.DEFAULT_Custodian_API_PORT}")

    beforeTest {
        KeyService.getService().listKeys().forEach{
            KeyService.getService().delete(it.keyId.id)
        }
        CustodianAPI.start()

    }

    afterTest {
        CustodianAPI.stop()
    }

    "Check Custodian Presentation generation LD_PROOF" {
        val did = DidService.create(DidMethod.key)

        // Issuance is Signatory stuff, we're just testing the Custodian here
        val vcJwt = Signatory.getService().issue(
            "VerifiableDiploma",
            ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018",
                proofType = ProofType.LD_PROOF
            )
        )

        val response: String =
            client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/credentials/present") {
                contentType(ContentType.Application.Json)
                setBody(PresentCredentialsRequest(listOf(vcJwt), did))
            }.bodyAsText()

        val vp = response.toCredential() as VerifiablePresentation

        vp.type shouldBe VerifiablePresentation.type

        println("VP Response: $response")

        Auditor.getService().verify(response, listOf(SignaturePolicy())).valid shouldBe true
    }

    "Check Custodian Presentation generation JWT" {
        val did = DidService.create(DidMethod.key)

        // Issuance is Signatory stuff, we're just testing the Custodian here
        val vcJwt = Signatory.getService().issue(
            "VerifiableDiploma",
            ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018",
                proofType = ProofType.JWT
            )
        )

        val response: String =
            client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/credentials/present") {
                contentType(ContentType.Application.Json)
                setBody(PresentCredentialsRequest(listOf(vcJwt), did))
            }.bodyAsText()

        response.count { it == '.' } shouldBe 2

        println("VP Response: $response")

        Auditor.getService().verify(response, listOf(SignaturePolicy())).valid shouldBe true
    }

    "Test export key" {
        forAll(
            row(KeyAlgorithm.ECDSA_Secp256k1, KeyFormat.JWK, true),
            row(KeyAlgorithm.EdDSA_Ed25519, KeyFormat.JWK, true),
            row(KeyAlgorithm.RSA, KeyFormat.JWK, true),
            row(KeyAlgorithm.ECDSA_Secp256k1, KeyFormat.JWK, false),
            row(KeyAlgorithm.EdDSA_Ed25519, KeyFormat.JWK, false),
            row(KeyAlgorithm.RSA, KeyFormat.JWK, false),
            row(KeyAlgorithm.ECDSA_Secp256k1, KeyFormat.PEM, true),
            row(KeyAlgorithm.EdDSA_Ed25519, KeyFormat.PEM, true),
            row(KeyAlgorithm.RSA, KeyFormat.PEM, true),
            row(KeyAlgorithm.ECDSA_Secp256k1, KeyFormat.PEM, false),
            row(KeyAlgorithm.EdDSA_Ed25519, KeyFormat.PEM, false),
            row(KeyAlgorithm.RSA, KeyFormat.PEM, false)
        ) { alg, format, isPrivate ->
            val kid = KeyService.getService().generate(alg)
            val response =
                runBlocking {
                    client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/keys/export") {
                        contentType(ContentType.Application.Json)
                        setBody(ExportKeyRequest(kid.id, format, isPrivate))
                    }.bodyAsText()
                }
            println(response)
            response shouldBe KeyService.getService()
                .export(kid.id, format, if (isPrivate) KeyType.PRIVATE else KeyType.PUBLIC)
        }
    }

    "Test delete key" {
        forAll(
            row(KeyAlgorithm.ECDSA_Secp256k1),
            row(KeyAlgorithm.EdDSA_Ed25519),
            row(KeyAlgorithm.RSA),
        ) { alg ->
            val kid = KeyService.getService().generate(alg)
            val response = runBlocking {
                client.delete("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/keys/${kid.id}")
            }
            response.status shouldBe HttpStatusCode.OK
            shouldThrow<Exception> {
                KeyService.getService().load(kid.id)
            }
        }
    }

    "Test import key JWK"{
        forAll(
//            Ed25519 Private
            row(readWhenContent(File("src/test/resources/cli/privKeyEd25519Jwk.json")).replace(Regex("(\r\n|\r|\n| )"), ""), KeyType.PRIVATE),
//            Ed25519 Public
            row(readWhenContent(File("src/test/resources/cli/pubKeyEd25519Jwk.json")).replace(Regex("(\r\n|\r|\n| )"), ""), KeyType.PUBLIC),
//            Secp256k1 Private
            row(readWhenContent(File("src/test/resources/key/privKeySecp256k1Jwk.json")).replace(Regex("(\r\n|\r|\n| )"), ""), KeyType.PRIVATE),
//            Secp256k1 Public
            row(readWhenContent(File("src/test/resources/key/pubKeySecp256k1Jwk.json")).replace(Regex("(\r\n|\r|\n| )"), ""), KeyType.PUBLIC),
//            RSA Private
            row(readWhenContent(File("src/test/resources/key/privkey.jwk")).replace(Regex("(\r\n|\r|\n| )"), ""), KeyType.PRIVATE),
//            RSA Public
            row(readWhenContent(File("src/test/resources/key/pubkey.jwk")).replace(Regex("(\r\n|\r|\n| )"), ""), KeyType.PUBLIC),
        ){ keyStr, type ->
            runBlocking {
                val response = client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/keys/import") {
                    setBody(keyStr)
                }
                val export = KeyService.getService().export(response.body<KeyId>().id, KeyFormat.JWK, type)
                export shouldBe keyStr
            }
        }
    }

    "Test import key PEM"{
        forAll(
//            RSA PEM
            row(readWhenContent(File("src/test/resources/key/rsa.pem"))),
//            Ed25519 PEM
            row(readWhenContent(File("src/test/resources/key/ed25519.pem"))),
//            Secp256k1 PEM
            row(readWhenContent(File("src/test/resources/key/secp256k1.pem"))),
        ) { keyStr ->
            runBlocking {
                val response = client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/keys/import") {
                    setBody(keyStr)
                }
                val priv = KeyService.getService().export(response.body<KeyId>().id, KeyFormat.PEM, KeyType.PRIVATE)
                val pub = KeyService.getService().export(response.body<KeyId>().id, KeyFormat.PEM, KeyType.PUBLIC)
                priv.plus(System.lineSeparator()).plus(pub) shouldBe keyStr
            }
        }
    }

    "Test delete did" {
        forAll(
            row(DidMethod.key, null),
            row(DidMethod.web, null),
            row(DidMethod.ebsi, null),
            row(DidMethod.key, KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1).id),
            row(DidMethod.key, KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519).id),
            row(DidMethod.key, KeyService.getService().generate(KeyAlgorithm.RSA).id),
            row(DidMethod.web, KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1).id),
            row(DidMethod.web, KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519).id),
            row(DidMethod.web, KeyService.getService().generate(KeyAlgorithm.RSA).id),
            row(DidMethod.ebsi, KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1).id),
            row(DidMethod.ebsi, KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519).id),
            row(DidMethod.ebsi, KeyService.getService().generate(KeyAlgorithm.RSA).id),
        ) { method, key ->
            val did = DidService.create(method, key)
            val response = runBlocking {
                client.delete("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/did/$did")
            }
            response.status shouldBe HttpStatusCode.OK
            shouldThrow<Exception> {
                DidService.load(did)
            }
        }
    }

    /*"Test documentation" {
        val response = get("/v1/api-documentation").readText()

        response shouldContain "\"operationId\":\"health\""
        response shouldContain "Returns HTTP 200 in case all services are up and running"
    }*/
})








