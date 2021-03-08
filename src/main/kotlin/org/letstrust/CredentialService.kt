package org.letstrust

import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.crypto.provider.Ed25519Provider
import info.weboftrust.ldsignatures.crypto.provider.impl.TinkEd25519Provider
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.signer.EcdsaSecp256k1Signature2019LdSigner
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.signer.Ed25519Signature2020LdSigner
import info.weboftrust.ldsignatures.verifier.EcdsaSecp256k1Signature2019LdVerifier
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2018LdVerifier
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2020LdVerifier
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.bitcoinj.core.ECKey
import org.json.JSONObject
import org.letstrust.model.*
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.streams.toList

private val log = KotlinLogging.logger {}

object CredentialService {

    // Supported signatures
    enum class SignatureType {
        Ed25519Signature2018,
        EcdsaSecp256k1Signature2019,
        Ed25519Signature2020
    }

    init {
        Ed25519Provider.set(TinkEd25519Provider())
    }

    fun sign(
        issuerDid: String,
        jsonCred: String,
        signatureType: SignatureType,
        domain: String? = null,
        nonce: String? = null
    ): String {

        log.debug { "Signing jsonLd object with: issuerDid ($issuerDid), signatureType ($signatureType), domain ($domain), nonce ($nonce)" }

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val issuerKeys = KeyManagementService.loadKeys(issuerDid)
        if (issuerKeys == null) {
            log.error { "Could not load signing key for $issuerDid" }
            throw Exception("Could not load signing key for $issuerDid")
        }

        val signer = when (signatureType) {
            SignatureType.Ed25519Signature2018 -> Ed25519Signature2018LdSigner(issuerKeys.getPrivateAndPublicKey())
            SignatureType.EcdsaSecp256k1Signature2019 -> EcdsaSecp256k1Signature2019LdSigner(ECKey.fromPrivate(issuerKeys.getPrivKey()))
            SignatureType.Ed25519Signature2020 -> Ed25519Signature2020LdSigner(issuerKeys.getPrivateAndPublicKey())
        }

        signer.creator = URI.create(issuerDid)
        signer.created = Date() // Use the current date
        signer.domain = domain
        signer.nonce = nonce

        val proof = signer.sign(jsonLdObject)

        // TODO Fix: this hack is needed as, signature-ld encodes type-field as array, which is not correct
        // return correctProofStructure(proof, jsonCred)
        return jsonLdObject.toJson(true)

    }

    private fun correctProofStructure(ldProof: LdProof, jsonCred: String): String {
        val vc = Json.decodeFromString<VerifiableCredential>(jsonCred)
        vc.proof = Proof(
            ldProof.type,
            LocalDateTime.ofInstant(ldProof.created.toInstant(), ZoneId.systemDefault()),
            ldProof.creator.toString(),
            ldProof.proofPurpose,
            null,
            ldProof.proofValue,
            ldProof.jws
        )
        return Json.encodeToString(vc)
    }

    fun addProof(credMap: Map<String, String>, ldProof: LdProof): String {
        val signedCredMap = HashMap<String, Any>(credMap)
        signedCredMap["proof"] = JSONObject(ldProof.toJson())
        return JSONObject(signedCredMap).toString()
    }

    fun verify(vc: String): Boolean {
        log.debug { "Verifying VC:\n$vc" }

        val vcObj = Json.decodeFromString<VerifiableCredential>(vc)
        log.trace { "VC decoded: $vcObj" }

        val signatureType = SignatureType.valueOf(vcObj.proof!!.type)
        log.debug { "Issuer: ${vcObj.issuer}" }
        log.debug { "Signature type: $signatureType" }

        val vcVerified = verify(vcObj.issuer, vc, signatureType)
        log.debug { "Verification of LD-Proof returned: $vcVerified" }
        return vcVerified
    }

    fun verifyVp(vp: String): Boolean {
        log.debug { "Verifying VP:\n$vp" }

        val vpObj = Json.decodeFromString<VerifiablePresentation>(vp)
        log.trace { "VC decoded: $vpObj" }

        val signatureType = SignatureType.valueOf(vpObj.proof!!.type)
        val issuer = vpObj.proof.creator!!
        log.debug { "Issuer: $issuer" }
        log.debug { "Signature type: $signatureType" }

        val vpVerified = verify(issuer, vp, signatureType)
        log.debug { "Verification of LD-Proof returned: $vpVerified" }
        return vpVerified
    }

    fun verify(issuerDid: String, vc: String, signatureType: SignatureType): Boolean {
        log.trace { "Loading verification key for:  $issuerDid" }
        val issuerKeys = KeyManagementService.loadKeys(issuerDid)
        if (issuerKeys == null) {
            log.error { "Could not load verification key for $issuerDid" }
            throw Exception("Could not load verification key for $issuerDid")
        }

        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true

        log.trace { "Document loader config: isEnableHttp (${confLoader.isEnableHttp}), isEnableHttps (${confLoader.isEnableHttps}), isEnableFile (${confLoader.isEnableFile}), isEnableLocalCache (${confLoader.isEnableLocalCache})" }

        val jsonLdObject = JsonLDObject.fromJson(vc)
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
        log.trace { "Decoded Json LD object: $jsonLdObject" }

        val verifier = when (signatureType) {
            SignatureType.Ed25519Signature2018 -> Ed25519Signature2018LdVerifier(issuerKeys.getPubKey())
            SignatureType.EcdsaSecp256k1Signature2019 -> EcdsaSecp256k1Signature2019LdVerifier(ECKey.fromPublicOnly(issuerKeys.getPubKey()))
            SignatureType.Ed25519Signature2020 -> Ed25519Signature2020LdVerifier(issuerKeys.getPubKey())
        }
        log.trace { "Loaded Json LD verifier with signature suite: ${verifier.signatureSuite}" }


        return verifier.verify(jsonLdObject)
    }

    fun present(vc: String, domain: String?, challenge: String?): String {
        log.debug { "Creating a presentation for VC:\n$vc" }
        val vcObj = Json.decodeFromString<VerifiableCredential>(vc)
        log.trace { "Decoded VC $vcObj" }

        val holderDid = vcObj.credentialSubject.id ?: vcObj.credentialSubject.did ?: throw Exception("Could not determine holder DID for $vcObj")

        log.debug { "Holder DID: $holderDid" }

        val vpReq = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), listOf(vcObj), null)
        val vpReqStr = Json { prettyPrint = true }.encodeToString(vpReq)
        log.trace { "VP request:\n$vpReq" }

//        val holderKeys = KeyManagementService.loadKeys(holderDid!!)
//        if (holderKeys == null) {
//            log.error { "Could not load authentication key for $holderDid" }
//            throw Exception("Could not load authentication key for $holderDid")
//        }

        val vp = sign(holderDid, vpReqStr, SignatureType.Ed25519Signature2018, domain, challenge)
        log.debug { "VP created:$vp" }
        return vp
    }

    fun listVCs(): List<String> {
        return Files.walk(Path.of("data/vc/created"))
            .filter { it -> Files.isRegularFile(it) }
            .filter { it -> it.toString().endsWith(".json") }
            .map { it.fileName.toString() }.toList()
    }

    fun defaultVcTemplate(): VerifiableCredential {
        return VerifiableCredential(
            listOf(
                "https://www.w3.org/2018/credentials/v1"
            ),
            "XXX",
            listOf("VerifiableCredential", "VerifiableAttestation"),
            "XXX",
            LocalDateTime.now().withNano(0),
            LocalDateTime.now().withNano(0),
            CredentialSubject(null, "XXX", null, listOf("claim1", "claim2")),
            CredentialStatus("https://essif.europa.eu/status", "CredentialsStatusList2020"),
            CredentialSchema("https://essif.europa.eu/tsr/education/CSR1224.json", "JsonSchemaValidator2018")
        )
    }
}
