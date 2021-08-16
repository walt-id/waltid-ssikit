package id.walt.services.vc

import com.danubetech.keyformats.crypto.provider.Ed25519Provider
import com.danubetech.keyformats.crypto.provider.impl.TinkEd25519Provider
import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.VcLibManager
import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.model.CredentialStatus
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiableAttestation
import id.walt.vclib.vclist.VerifiablePresentation
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import mu.KotlinLogging
import org.json.JSONObject
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.LdSigner
import id.walt.services.essif.EssifServer.nonce
import id.walt.services.essif.TrustedIssuerClient.domain
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vc.VcUtils.getIssuer
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

private val log = KotlinLogging.logger {}

open class WaltIdJsonLdCredentialService : JsonLdCredentialService() {

    private var ks: KeyStoreService = KeyStoreService.getService()

    init {
        Ed25519Provider.set(TinkEd25519Provider())
    }

    override fun sign(
        jsonCred: String,
        config: ProofConfig
    ): String {
        log.debug { "Signing jsonLd object with: issuerDid (${config.issuerDid}), domain (${config.domain}), nonce (${config.nonce}" }

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val key = ks.load(config.issuerDid)

        val signer = when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> LdSigner.EcdsaSecp256k1Signature2019(key.keyId)
            KeyAlgorithm.EdDSA_Ed25519 -> LdSigner.Ed25519Signature2018(key.keyId)
            else -> throw Exception("Signature for key algorithm ${key.algorithm} not supported")
        }

        signer.creator = URI.create(config.issuerDid)
        signer.created = Date() // Use the current date
        signer.domain = domain
        signer.nonce = nonce
        config.issuerVerificationMethod?.let { signer.verificationMethod = URI.create(config.issuerVerificationMethod) }
        signer.proofPurpose = config.proofPurpose


        log.debug { "Signing: $jsonLdObject" }
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
            KeyAlgorithm.ECDSA_Secp256k1 -> id.walt.crypto.LdVerifier.EcdsaSecp256k1Signature2019(publicKey.getPublicKey())
            KeyAlgorithm.EdDSA_Ed25519 -> id.walt.crypto.LdVerifier.Ed25519Signature2018(publicKey)
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

//    private fun correctProofStructure(ldProof: LdProof, jsonCred: String): String {
//        val vc = Klaxon().parse<VerifiableCredential>(jsonCred)
//        vc.proof = Proof( /* TODO @Phil which Proof? */
//            ldProof.type,
//            LocalDateTime.ofInstant(ldProof.created.toInstant(), ZoneId.systemDefault()),
//            ldProof.creator.toString(),
//            ldProof.proofPurpose,
//            null,
//            ldProof.proofValue,
//            ldProof.jws
//        )
//        return Klaxon().toJsonString(vc)
//    }

    override fun addProof(credMap: Map<String, String>, ldProof: LdProof): String {
        val signedCredMap = HashMap<String, Any>(credMap)
        signedCredMap["proof"] = JSONObject(ldProof.toJson())
        return JSONObject(signedCredMap).toString()
    }

    fun verifyVerifiableCredential(json: String) =
        VerificationResult(verifyVc(json), VerificationType.VERIFIABLE_CREDENTIAL)

    fun verifyVerifiablePresentation(json: String) =
        VerificationResult(verifyVp(json), VerificationType.VERIFIABLE_PRESENTATION)

    override fun verify(vcOrVp: String): VerificationResult = when (VcLibManager.getVerifiableCredential(vcOrVp)) {
        is VerifiablePresentation -> verifyVerifiablePresentation(vcOrVp)
        else -> verifyVerifiableCredential(vcOrVp)
    }


    override fun verifyVc(vc: String): Boolean {
        log.debug { "Verifying VC:\n$vc" }

        //val vcObj = Klaxon().parse<VerifiableCredential>(vc)
        val vcObj = vc.toCredential()
        log.trace { "VC decoded: $vcObj" }

//        val signatureType = SignatureType.valueOf(vcObj.proof!!.type)
//        log.debug { "Issuer: ${vcObj.issuer}" }
//        log.debug { "Signature type: $signatureType" }

        val issuer = getIssuer(vcObj)

        val vcVerified = verifyVc(issuer, vc)
        log.debug { "Verification of LD-Proof returned: $vcVerified" }
        return vcVerified
    }

    override fun verifyVp(vpJson: String): Boolean {
        log.debug { "Verifying VP:\n$vpJson" }

        // val vpObj = Klaxon().parse<VerifiablePresentation>(vp)
        log.trace { "VC decoded: ${vpJson.toCredential()}" }


//        val signatureType = SignatureType.valueOf(vpObj.proof!!.type)
//        val presenter = vpObj.proof.creator!!
//        log.debug { "Presenter: $presenter" }
//        log.debug { "Signature type: $signatureType" }
//

        val vpVerified = verifyVc((vpJson.toCredential() as VerifiablePresentation).proof!!.creator!!, vpJson)
        log.debug { "Verification of VP-Proof returned: $vpVerified" }
//
//        // TODO remove legacy verifiableCredential
//        val (vcStr, issuer) = if (vpObj.verifiableCredential != null) {
//            val vc = vpObj.verifiableCredential.get(0)
//            Pair(vc.encodePretty(), vc.issuer)
//        } else {
//            val vc = vpObj.vc!!.get(0)
//            Pair(Klaxon().toJsonString(vc), vc.issuer!!)
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

    override fun present(vc: String, domain: String?, challenge: String?): String {
        log.debug { "Creating a presentation for VC:\n$vc" }

//        val (vpReqStr, holderDid) = try {
//            val eurpass = VC.decode(vc) as Europass
//            val vpReq = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), null, listOf(eurpass), null)
//            val vpReqStr = Klaxon().toJsonString(vpReq)
//            Pair(vpReqStr, eurpass.credentialSubject!!.id!!)
//        } catch (e: IllegalArgumentException) {
//            // TODO: get rid of legacy code
//            val vc = Klaxon().parse<VerifiableCredential>(vc)
//            val vpReq = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), listOf(vc), null)
//            val vpReqStr = Klaxon().toJsonString(vpReq)
//            log.trace { "VP request:\n$vpReq" }
//            Pair(vpReqStr, (vc.credentialSubject.id ?: vc.credentialSubject.did)!!)
//        }


        val vpReqStr = VerifiablePresentation(
            id = "id",
            verifiableCredential = listOf(vc.toCredential())
        ).encode()

        log.trace { "VP request:\n$vpReqStr" }

//        val holderKeys = KeyManagementService.loadKeys(holderDid!!)
//        if (holderKeys == null) {
//            log.error { "Could not load authentication key for $holderDid" }
//            throw Exception("Could not load authentication key for $holderDid")
//        }

        val holderDid = VcUtils.getHolder(vc.toCredential())
        val proofConfig = ProofConfig(
            issuerDid = holderDid,
            subjectDid = holderDid,
            domain = domain,
            proofType = ProofType.LD_PROOF,
            nonce = challenge
        )
        val vp = sign(vpReqStr, proofConfig)
        // val vp = sign(VcUtils.getHolder(vc.toCredential()), vpReqStr, domain, challenge)
        log.debug { "VP created:$vp" }
        return vp
    }

    override fun listVCs(): List<String> {
        return Files.walk(Path.of("data/vc/created"))
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".json") }
            .map { it.fileName.toString() }.toList()
    }

    override fun defaultVcTemplate(): VerifiableCredential {
        return VerifiableAttestation(
            context = listOf(
                "https://www.w3.org/2018/credentials/v1",
//                "https://essif.europa.eu/schemas/v-a/2020/v1",
//                "https://essif.europa.eu/schemas/eidas/2020/v1"
            ),
            id = "education#higherEducation#3fea53a4-0432-4910-ac9c-69ah8da3c37f",
            issuer = "did:ebsi:2757945549477fc571663bee12042873fe555b674bd294a3",
            issuanceDate = "2019-06-22T14:11:44Z",
            validFrom = "2019-06-22T14:11:44Z",
            credentialSubject = VerifiableAttestation.CredentialSubject(
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
    }

    /*override fun listTemplates(): List<String> {
        return Files.walk(Path.of("templates"))
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".json") }
            .map { it.fileName.toString().replace("vc-template-", "").replace(".json", "") }.toList()
    }

    //TODO: fix typed response: fun loadTemplate(name: String): VerifiableCredential = Klaxon().parse(File("templates/vc-template-$name.json").readText())
    override fun loadTemplate(name: String): String = File("templates/vc-template-$name.json").readText()*/
}
