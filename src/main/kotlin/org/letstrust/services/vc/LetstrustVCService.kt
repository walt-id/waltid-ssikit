package org.letstrust.services.vc

import com.danubetech.keyformats.crypto.provider.Ed25519Provider
import com.danubetech.keyformats.crypto.provider.impl.TinkEd25519Provider
import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.json.JSONObject
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.LdSigner
import org.letstrust.model.*
import org.letstrust.model.Proof
import org.letstrust.services.keystore.KeyStoreService
import org.letstrust.vclib.vcs.*
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.streams.toList

private val log = KotlinLogging.logger {}

open class LetstrustVCService : VCService() {

    private var ks: KeyStoreService = KeyStoreService.getService()

    init {
        Ed25519Provider.set(TinkEd25519Provider())
    }

    override fun sign(
        issuerDid: String,
        jsonCred: String,
        domain: String?,
        nonce: String?,
        verificationMethod: String?,
        proofPurpose: String?
    ): String {
        log.debug { "Signing jsonLd object with: issuerDid ($issuerDid), domain ($domain), nonce ($nonce)" }

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val key = ks.load(issuerDid)

        val signer = when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> LdSigner.EcdsaSecp256k1Signature2019(key.keyId)
            KeyAlgorithm.EdDSA_Ed25519 -> LdSigner.Ed25519Signature2018(key.keyId)
            else -> throw Exception("Signature for key algorithm ${key.algorithm} not supported")
        }

        signer.creator = URI.create(issuerDid)
        signer.created = Date() // Use the current date
        signer.domain = domain
        signer.nonce = nonce
        verificationMethod?.let { signer.verificationMethod = URI.create(verificationMethod) }
        signer.proofPurpose = proofPurpose


        val proof = signer.sign(jsonLdObject)

        // TODO Fix: this hack is needed as, signature-ld encodes type-field as array, which is not correct
        // return correctProofStructure(proof, jsonCred)
        return jsonLdObject.toJson(true)

    }

    override fun verifyVc(issuerDid: String, vc: String): Boolean {
        log.trace { "Loading verification key for:  $issuerDid" }

        val publicKey = ks.load(issuerDid)

        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true

        log.trace { "Document loader config: isEnableHttp (${confLoader.isEnableHttp}), isEnableHttps (${confLoader.isEnableHttps}), isEnableFile (${confLoader.isEnableFile}), isEnableLocalCache (${confLoader.isEnableLocalCache})" }

        val jsonLdObject = JsonLDObject.fromJson(vc)
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
        log.trace { "Decoded Json LD object: $jsonLdObject" }

        val verifier = when (publicKey.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> org.letstrust.crypto.LdVerifier.EcdsaSecp256k1Signature2019(publicKey.getPublicKey())
            KeyAlgorithm.EdDSA_Ed25519 -> org.letstrust.crypto.LdVerifier.Ed25519Signature2018(publicKey)
            else -> throw Exception("Signature for key algorithm ${publicKey.algorithm} not supported")
        }

        log.trace { "Loaded Json LD verifier with signature suite: ${verifier.signatureSuite}" }

        return verifier.verify(jsonLdObject)
    }


    //TODO: following methods might be depreciated


//    fun sign_old(
//        issuerDid: String,
//        jsonCred: String,
//        signatureType: SignatureType,
//        domain: String? = null,
//        nonce: String? = null
//    ): String {
//
//        log.debug { "Signing jsonLd object with: issuerDid ($issuerDid), signatureType ($signatureType), domain ($domain), nonce ($nonce)" }
//
//        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
//        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader
//
//        confLoader.isEnableHttp = true
//        confLoader.isEnableHttps = true
//        confLoader.isEnableFile = true
//        confLoader.isEnableLocalCache = true
//        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
//
//        val issuerKeys = KeyManagementService.loadKeys(issuerDid)
//        if (issuerKeys == null) {
//            log.error { "Could not load signing key for $issuerDid" }
//            throw Exception("Could not load signing key for $issuerDid")
//        }
//
//        val signer = when (signatureType) {
//            SignatureType.Ed25519Signature2018 -> Ed25519Signature2018LdSigner(issuerKeys.getPrivateAndPublicKey())
//            SignatureType.EcdsaSecp256k1Signature2019 -> EcdsaSecp256k1Signature2019LdSigner(ECKey.fromPrivate(issuerKeys.getPrivKey()))
//            SignatureType.Ed25519Signature2020 -> Ed25519Signature2020LdSigner(issuerKeys.getPrivateAndPublicKey())
//        }
//
//        signer.creator = URI.create(issuerDid)
//        signer.created = Date() // Use the current date
//        signer.domain = domain
//        signer.nonce = nonce
//
//        val proof = signer.sign(jsonLdObject)
//
//        // TODO Fix: this hack is needed as, signature-ld encodes type-field as array, which is not correct
//        // return correctProofStructure(proof, jsonCred)
//        return jsonLdObject.toJson(true)
//
//    }

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

    override fun addProof(credMap: Map<String, String>, ldProof: LdProof): String {
        val signedCredMap = HashMap<String, Any>(credMap)
        signedCredMap["proof"] = JSONObject(ldProof.toJson())
        return JSONObject(signedCredMap).toString()
    }

    override fun verify(vcOrVp: String): VerificationResult {
        return when { // TODO: replace the raw contains call
            vcOrVp.contains("VerifiablePresentation") -> VerificationResult(
                verifyVp(vcOrVp),
                VerificationType.VERIFIABLE_PRESENTATION
            )
            else -> VerificationResult(verifyVc(vcOrVp), VerificationType.VERIFIABLE_CREDENTIAL)
        }
    }


    override fun verifyVc(vc: String): Boolean {
        log.debug { "Verifying VC:\n$vc" }

        //val vcObj = Json.decodeFromString<VerifiableCredential>(vc)
        val vcObj = VC.decode(vc)
        log.trace { "VC decoded: $vcObj" }

//        val signatureType = SignatureType.valueOf(vcObj.proof!!.type)
//        log.debug { "Issuer: ${vcObj.issuer}" }
//        log.debug { "Signature type: $signatureType" }

        val vcVerified = verifyVc(vcObj.issuer(), vc)
        log.debug { "Verification of LD-Proof returned: $vcVerified" }
        return vcVerified
    }

    override fun verifyVp(vpStr: String): Boolean {
        log.debug { "Verifying VP:\n$vpStr" }

        // val vpObj = Json.decodeFromString<VerifiablePresentation>(vp)
        val vp = VC.decode(vpStr)
        log.trace { "VC decoded: $vp" }


//        val signatureType = SignatureType.valueOf(vpObj.proof!!.type)
//        val presenter = vpObj.proof.creator!!
//        log.debug { "Presenter: $presenter" }
//        log.debug { "Signature type: $signatureType" }
//
        val vpVerified = verifyVc(vp.issuer(), vpStr)
        log.debug { "Verification of VP-Proof returned: $vpVerified" }
//
//        // TODO remove legacy verifiableCredential
//        val (vcStr, issuer) = if (vpObj.verifiableCredential != null) {
//            val vc = vpObj.verifiableCredential.get(0)
//            Pair(vc.encodePretty(), vc.issuer)
//        } else {
//            val vc = vpObj.vc!!.get(0)
//            Pair(Json.encodeToString(vc), vc.issuer!!)
//        }

//        log.debug { "Verifying VC:\n$vp" }
//        val vcVerified = verifyVc(vp.issuer(), vpStr)

//        log.debug { "Verification of VC-Proof returned: $vcVerified" }

        return vpVerified // TODO add vc verification && vcVerified
    }

//    fun verify_old(issuerDid: String, vc: String, signatureType: SignatureType): Boolean {
//        log.trace { "Loading verification key for:  $issuerDid" }
//        val issuerKeys = KeyManagementService.loadKeys(issuerDid)
//        if (issuerKeys == null) {
//            log.error { "Could not load verification key for $issuerDid" }
//            throw Exception("Could not load verification key for $issuerDid")
//        }
//
//        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader
//
//        confLoader.isEnableHttp = true
//        confLoader.isEnableHttps = true
//        confLoader.isEnableFile = true
//        confLoader.isEnableLocalCache = true
//
//        log.trace { "Document loader config: isEnableHttp (${confLoader.isEnableHttp}), isEnableHttps (${confLoader.isEnableHttps}), isEnableFile (${confLoader.isEnableFile}), isEnableLocalCache (${confLoader.isEnableLocalCache})" }
//
//        val jsonLdObject = JsonLDObject.fromJson(vc)
//        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
//        log.trace { "Decoded Json LD object: $jsonLdObject" }
//
//        val verifier = when (signatureType) {
//            SignatureType.Ed25519Signature2018 -> Ed25519Signature2018LdVerifier(issuerKeys.getPubKey())
//            SignatureType.EcdsaSecp256k1Signature2019 -> EcdsaSecp256k1Signature2019LdVerifier(ECKey.fromPublicOnly(issuerKeys.getPubKey()))
//            SignatureType.Ed25519Signature2020 -> Ed25519Signature2020LdVerifier(issuerKeys.getPubKey())
//        }
//        log.trace { "Loaded Json LD verifier with signature suite: ${verifier.signatureSuite}" }
//
//        return verifier.verify(jsonLdObject)
//    }

    override fun present(vcStr: String, domain: String?, challenge: String?): String {
        log.debug { "Creating a presentation for VC:\n$vcStr" }

//        val (vpReqStr, holderDid) = try {
//            val eurpass = VC.decode(vc) as Europass
//            val vpReq = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), null, listOf(eurpass), null)
//            val vpReqStr = Json { prettyPrint = true }.encodeToString(vpReq)
//            Pair(vpReqStr, eurpass.credentialSubject!!.id!!)
//        } catch (e: IllegalArgumentException) {
//            // TODO: get rid of legacy code
//            val vc = Json.decodeFromString<VerifiableCredential>(vc)
//            val vpReq = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), listOf(vc), null)
//            val vpReqStr = Json { prettyPrint = true }.encodeToString(vpReq)
//            log.trace { "VP request:\n$vpReq" }
//            Pair(vpReqStr, (vc.credentialSubject.id ?: vc.credentialSubject.did)!!)
//        }


        val vc = VC.decode(vcStr)

        val vpReqStr = when {
            vc is Europass -> EuropassVP(
                listOf("https://www.w3.org/2018/credentials/v1"),
                listOf("VerifiablePresentation"),
                null,
                listOf(vc),
                null
            ).encode()
            vc is PermanentResidentCard -> PermanentResidentCardVP(
                listOf("https://www.w3.org/2018/credentials/v1"),
                listOf("VerifiablePresentation"),
                null,
                listOf(vc),
                null
            ).encode()
            vc is EbsiVerifiableAttestation -> EbsiVerifiableAttestationVP(
                listOf("https://www.w3.org/2018/credentials/v1"),
                listOf("VerifiablePresentation"),
                null,
                listOf(vc),
                null
            ).encode()
            vc is EbsiVerifiableAuthorisation -> EbsiVerifiableAuthorisationVP(
                listOf("https://www.w3.org/2018/credentials/v1"),
                listOf("VerifiablePresentation"),
                null,
                listOf(vc),
                null
            ).encode()
            else -> throw IllegalArgumentException("VC type not supported")
        }

        log.trace { "VP request:\n$vpReqStr" }

//        val holderKeys = KeyManagementService.loadKeys(holderDid!!)
//        if (holderKeys == null) {
//            log.error { "Could not load authentication key for $holderDid" }
//            throw Exception("Could not load authentication key for $holderDid")
//        }

        val vp = sign(vc.holder(), vpReqStr, domain, challenge)
        log.debug { "VP created:$vp" }
        return vp
    }

    override fun listVCs(): List<String> {
        return Files.walk(Path.of("data/vc/created"))
            .filter { it -> Files.isRegularFile(it) }
            .filter { it -> it.toString().endsWith(".json") }
            .map { it.fileName.toString() }.toList()
    }

    override fun defaultVcTemplate(): VerifiableCredential {
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

    override fun listTemplates(): List<String> {
        return Files.walk(Path.of("templates"))
            .filter { it -> Files.isRegularFile(it) }
            .filter { it -> it.toString().endsWith(".json") }
            .map { it.fileName.toString().replace("vc-template-", "").replace(".json", "") }.toList()
    }

    //TODO: fix typed response: fun loadTemplate(name: String): VerifiableCredential = Json.decodeFromString(File("templates/vc-template-$name.json").readText())
    override fun loadTemplate(name: String): String = File("templates/vc-template-$name.json").readText()
}
