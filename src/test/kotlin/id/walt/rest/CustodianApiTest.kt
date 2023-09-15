package id.walt.rest

import id.walt.auditor.Auditor
import id.walt.auditor.policies.SignaturePolicy
import id.walt.common.readWhenContent
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.credentials.w3c.toVerifiablePresentation
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.model.DidMethod
import id.walt.rest.custodian.CustodianAPI
import id.walt.rest.custodian.ExportKeyRequest
import id.walt.rest.custodian.PresentCredentialsRequest
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices.httpNoAuth
import id.walt.services.did.DidService
import id.walt.services.did.DidWebCreateOptions
import id.walt.services.key.KeyFormat
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File

class CustodianApiTest : StringSpec({

    ServiceMatrix("service-matrix.properties")
    val webOptions = DidWebCreateOptions("walt.id")
    val client = httpNoAuth

    println("${CustodianAPI.DEFAULT_BIND_ADDRESS}/${CustodianAPI.DEFAULT_Custodian_API_PORT}")

    beforeTest {
        KeyService.getService().listKeys().forEach {
            KeyService.getService().delete(it.keyId.id)
        }
        CustodianAPI.start()

    }

    afterTest {
        CustodianAPI.stop()
    }

    "Check Custodian Presentation generation LD_PROOF" {
        val did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.verificationMethod!!.first().id

        // Issuance is Signatory stuff, we're just testing the Custodian here
        val vcJwt = Signatory.getService().issue(
            "VerifiableDiploma",
            ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = vm,
                proofType = ProofType.LD_PROOF
            )
        )

        val response: String =
            client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/credentials/present") {
                contentType(ContentType.Application.Json)
                setBody(PresentCredentialsRequest(listOf(vcJwt), did))
            }.bodyAsText()

        val vp = response.toVerifiablePresentation()

        vp shouldBe instanceOf<VerifiablePresentation>()

        println("VP Response: $response")

        Auditor.getService().verify(response, listOf(SignaturePolicy())).result shouldBe true
    }

    "Check Custodian Presentation generation JWT" {
        val did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        // Issuance is Signatory stuff, we're just testing the Custodian here
        val vcJwt = Signatory.getService().issue(
            "VerifiableDiploma",
            ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = vm,
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

        Auditor.getService().verify(response, listOf(SignaturePolicy())).result shouldBe true
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

    "Test import key JWK" {
        forAll(
//            Ed25519 Private
            row(
                readWhenContent(File("src/test/resources/cli/privKeyEd25519Jwk.json")).replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PRIVATE
            ),
//            Ed25519 Public
            row(
                readWhenContent(File("src/test/resources/cli/pubKeyEd25519Jwk.json")).replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PUBLIC
            ),
//            Secp256k1 Private
            row(
                readWhenContent(File("src/test/resources/key/privKeySecp256k1Jwk.json")).replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PRIVATE
            ),
//            Secp256k1 Public
            row(
                readWhenContent(File("src/test/resources/key/pubKeySecp256k1Jwk.json")).replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PUBLIC
            ),
//            RSA Private
            row(
                readWhenContent(File("src/test/resources/key/privkey.jwk")).replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PRIVATE
            ),
//            RSA Public
            row(
                readWhenContent(File("src/test/resources/key/pubkey.jwk")).replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PUBLIC
            ),
        ) { keyStr, type ->
            runBlocking {
                val response =
                    client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/keys/import") {
                        setBody(keyStr)
                    }
                val export = KeyService.getService().export(response.body<KeyId>().id, KeyFormat.JWK, type)
                export shouldBe keyStr
            }
        }
    }

    "Test import key PEM" {
        forAll(
//            RSA PEM
            row(readWhenContent(File("src/test/resources/key/pem/rsa/rsa.pem"))),
//            Ed25519 PEM
            row(readWhenContent(File("src/test/resources/key/pem/ed25519/ed25519.pem"))),
//            Secp256k1 PEM
            row(readWhenContent(File("src/test/resources/key/pem/ecdsa/secp256k1.pem"))),
        ) { keyStr ->
            runBlocking {
                val response =
                    client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/keys/import") {
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
            row(DidMethod.key, null, null),
            row(DidMethod.web, null, webOptions),
            row(DidMethod.ebsi, null, null),
            row(DidMethod.key, KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1).id, null),
            row(DidMethod.key, KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519).id, null),
            row(DidMethod.key, KeyService.getService().generate(KeyAlgorithm.RSA).id, null),
            row(DidMethod.web, KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1).id, webOptions),
            row(DidMethod.web, KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519).id, webOptions),
            row(DidMethod.web, KeyService.getService().generate(KeyAlgorithm.RSA).id, webOptions),
            row(DidMethod.ebsi, KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1).id, null),
            row(DidMethod.ebsi, KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519).id, null),
            row(DidMethod.ebsi, KeyService.getService().generate(KeyAlgorithm.RSA).id, null),
        ) { method, key, options ->
            val did = DidService.create(method, key, options)
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








