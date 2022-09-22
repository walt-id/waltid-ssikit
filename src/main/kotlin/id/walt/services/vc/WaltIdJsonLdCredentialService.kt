package id.walt.services.vc

import com.apicatalog.jsonld.JsonLdErrorCode
import com.danubetech.keyformats.crypto.provider.Ed25519Provider
import com.danubetech.keyformats.crypto.provider.impl.TinkEd25519Provider
import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDException
import foundation.identity.jsonld.JsonLDObject
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.LdSignatureType
import id.walt.crypto.LdSigner
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.essif.EssifServer.nonce
import id.walt.services.essif.TrustedIssuerClient.domain
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyStoreService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.vclib.credentials.VerifiableAttestation
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.*
import id.walt.vclib.schema.SchemaService
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.verifier.LdVerifier
import mu.KotlinLogging
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

open class WaltIdJsonLdCredentialService : JsonLdCredentialService() {

    private val keyStore: KeyStoreService
        get() = ContextManager.keyStore

    init {
        Ed25519Provider.set(TinkEd25519Provider())
    }

    private fun selectLdSigner(config: ProofConfig, key: Key): info.weboftrust.ldsignatures.signer.LdSigner<*> {
        return if(config.ldSignatureType != null) {
            when(config.ldSignatureType) {
                LdSignatureType.EcdsaSecp256k1Signature2019 -> {
                    require(key.algorithm == KeyAlgorithm.ECDSA_Secp256k1) { "Unsupported key algorithm ${key.algorithm} for ld signature type ${config.ldSignatureType}" };
                    LdSigner.EcdsaSecp256K1Signature2019(key.keyId)
                }
                LdSignatureType.Ed25519Signature2018 -> {
                    require(key.algorithm == KeyAlgorithm.EdDSA_Ed25519) { "Unsupported key algorithm ${key.algorithm} for ld signature type ${config.ldSignatureType}" }
                    LdSigner.Ed25519Signature2018(key.keyId)
                }
                LdSignatureType.Ed25519Signature2020 -> {
                    require(key.algorithm == KeyAlgorithm.EdDSA_Ed25519) { "Unsupported key algorithm ${key.algorithm} for ld signature type ${config.ldSignatureType}" }
                    LdSigner.Ed25519Signature2020(key.keyId)
                }
                LdSignatureType.JcsEd25519Signature2020 -> {
                    require(key.algorithm == KeyAlgorithm.EdDSA_Ed25519) { "Unsupported key algorithm ${key.algorithm} for ld signature type ${config.ldSignatureType}" }
                    LdSigner.JcsEd25519Signature2020(key.keyId)
                }
                LdSignatureType.JsonWebSignature2020 -> LdSigner.JsonWebSignature2020(key.keyId)
                LdSignatureType.RsaSignature2018 -> {
                    require(key.algorithm == KeyAlgorithm.RSA) { "Unsupported key algorithm ${key.algorithm} for ld signature type ${config.ldSignatureType}" }
                    LdSigner.RsaSignature2018(key.keyId)
                }
            }
        } else {
            LdSigner.JsonWebSignature2020(key.keyId)
        }
    }

    private fun selectLdVerifier(ldSignatureType: LdSignatureType, publicKey: Key): LdVerifier<*> {
        return when(ldSignatureType) {
            LdSignatureType.RsaSignature2018 -> {
                require(publicKey.algorithm == KeyAlgorithm.RSA) { "Unsupported key algorithm ${publicKey.algorithm} for ld signature type ${ldSignatureType}" }
                id.walt.crypto.LdVerifier.RsaSignature2018(publicKey)
            }
            LdSignatureType.JcsEd25519Signature2020 -> {
                require(publicKey.algorithm == KeyAlgorithm.EdDSA_Ed25519) { "Unsupported key algorithm ${publicKey.algorithm} for ld signature type ${ldSignatureType}" }
                id.walt.crypto.LdVerifier.JcsEd25519Signature2020(publicKey)
            }
            LdSignatureType.Ed25519Signature2020 -> {
                require(publicKey.algorithm == KeyAlgorithm.EdDSA_Ed25519) { "Unsupported key algorithm ${publicKey.algorithm} for ld signature type ${ldSignatureType}" }
                id.walt.crypto.LdVerifier.Ed25519Signature2020(publicKey)
            }
            LdSignatureType.Ed25519Signature2018 -> {
                require(publicKey.algorithm == KeyAlgorithm.EdDSA_Ed25519) { "Unsupported key algorithm ${publicKey.algorithm} for ld signature type ${ldSignatureType}" }
                id.walt.crypto.LdVerifier.Ed25519Signature2018(publicKey)
            }
            LdSignatureType.EcdsaSecp256k1Signature2019 -> {
                require(publicKey.algorithm == KeyAlgorithm.ECDSA_Secp256k1) { "Unsupported key algorithm ${publicKey.algorithm} for ld signature type ${ldSignatureType}" }
                id.walt.crypto.LdVerifier.EcdsaSecp256k1Signature2019(publicKey)
            }
            LdSignatureType.JsonWebSignature2020 -> id.walt.crypto.LdVerifier.JsonWebSignature2020(publicKey)
        }
    }

    override fun sign(jsonCred: String, config: ProofConfig): String {
        log.debug { "Signing jsonLd object with: issuerDid (${config.issuerDid}), domain (${config.domain}), nonce (${config.nonce}" }

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val vm = config.issuerVerificationMethod ?: config.issuerDid
        val key = keyStore.load(vm)

        val signer = selectLdSigner(config, key)

        signer.creator = URI.create(config.issuerDid)
        signer.created = Date() // Use the current date
        signer.domain = config.domain ?: domain
        signer.nonce = config.nonce ?: nonce
        config.issuerVerificationMethod?.let { signer.verificationMethod = URI.create(config.issuerVerificationMethod) }
        signer.proofPurpose = config.proofPurpose


        log.debug { "Signing: $jsonLdObject" }
        val proof = try {
            signer.sign(jsonLdObject)
        } catch (ldExc: JsonLDException) {
            if(ldExc.code == JsonLdErrorCode.LOADING_REMOTE_CONTEXT_FAILED) {
                // if JSON LD remote context failed to load, retry once
                log.warn { "JSON LD remote context failed to load, retrying once..." }
                signer.sign(jsonLdObject)
            } else {
                throw ldExc
            }
        }

        // TODO Fix: this hack is needed as, signature-ld encodes type-field as array, which is not correct
        // return correctProofStructure(proof, jsonCred)
        return jsonLdObject.toJson(true)

    }

    private fun getVerificationTypeFor(vcOrVp: VerifiableCredential): VerificationType = when(vcOrVp) {
        is VerifiablePresentation -> VerificationType.VERIFIABLE_PRESENTATION
        else -> VerificationType.VERIFIABLE_CREDENTIAL
    }

    override fun verify(vcOrVp: String): VerificationResult {
        val vcObj = vcOrVp.toCredential()
        val issuer = vcObj.issuer ?: throw Exception("No issuer DID found for VC or VP")
        val vm = vcObj.proof?.verificationMethod ?: issuer

        if(!DidService.importKeys(issuer)) {
            throw Exception("Could not resolve verification keys")
        }

        log.debug { "Loading verification key for:  $vm" }

        val publicKey = keyStore.load(vm)

        log.debug { "Verification key for:  $vm is: $publicKey" }

        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true

        log.debug { "Document loader config: isEnableHttp (${confLoader.isEnableHttp}), isEnableHttps (${confLoader.isEnableHttps}), isEnableFile (${confLoader.isEnableFile}), isEnableLocalCache (${confLoader.isEnableLocalCache})" }

        val jsonLdObject = JsonLDObject.fromJson(vcOrVp)
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
        log.debug { "Decoded Json LD object: $jsonLdObject" }

        val ldProof = LdProof.getFromJsonLDObject(jsonLdObject)
        if(ldProof == null) {
            log.info { "No LD proof found on VC" }
            throw Exception("No LD proof found on VC")
        }

        val ldSignatureType = LdSignatureType.valueOf(ldProof.type)

        val verifier = selectLdVerifier(ldSignatureType, publicKey)

        log.debug { "Loaded Json LD verifier with signature suite: ${verifier.signatureSuite}" }

        val verificatioResult = try {
            verifier.verify(jsonLdObject)
        } catch (ldExc: JsonLDException) {
            if(ldExc.code == JsonLdErrorCode.LOADING_REMOTE_CONTEXT_FAILED) {
                // if JSON LD remote context failed to load, retry once
                log.warn { "JSON LD remote context failed to load, retrying once..." }
                verifier.verify(jsonLdObject)
            } else {
                throw ldExc
            }
        }

        log.debug { "Json LD verifier returned: $verificatioResult" }

        return VerificationResult(verificatioResult, getVerificationTypeFor(vcObj))
    }

    override fun addProof(credMap: Map<String, String>, ldProof: LdProof): String {
        val signedCredMap = HashMap<String, Any>(credMap)
        signedCredMap["proof"] = JSONObject(ldProof.toJson())
        return JSONObject(signedCredMap).toString()
    }

    override fun present(
        vcs: List<String>,
        holderDid: String,
        domain: String?,
        challenge: String?,
        expirationDate: Instant?
    ): String {
        log.debug { "Creating a presentation for VCs:\n$vcs" }

        val id = "urn:uuid:${UUID.randomUUID()}"
        val config = ProofConfig(
            issuerDid = holderDid,
            issuerVerificationMethod = DidService.getAuthenticationMethods(holderDid)!![0].id,
            proofPurpose = "authentication",
            proofType = ProofType.LD_PROOF,
            domain = domain,
            nonce = challenge,
            credentialId = id,
            expirationDate = expirationDate
        )
        val vpReqStr =
            VerifiablePresentation(id = id, holder = holderDid, verifiableCredential = vcs.map { it.toCredential() }).encode()

        log.trace { "VP request: $vpReqStr" }
        log.trace { "Proof config: $$config" }

        val vp = sign(vpReqStr, config)

        log.debug { "VP created:$vp" }
        return vp
    }

    override fun listVCs(): List<String> {
        return Files.walk(Path.of("data/vc/created")).filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".json") }.map { it.fileName.toString() }.toList()
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
            issued = "2019-06-22T14:11:44Z",
            validFrom = "2019-06-22T14:11:44Z",
            credentialSubject = VerifiableAttestation.VerifiableAttestationSubject(
                id = "id123"
            ),
            credentialStatus = CredentialStatus(
                id = "https://essif.europa.eu/status/identity#verifiableID#1dee355d-0432-4910-ac9c-70d89e8d674e",
                type = "CredentialStatusList2020"
            ),
            credentialSchema = CredentialSchema(
                id = "https://essif.europa.eu/tsr-vid/verifiableid1.json", type = "JsonSchemaValidator2018"
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

    override fun validateSchema(vc: VerifiableCredential, schema: String): Boolean {
        val results = SchemaService.validateSchema(vc.json!!, schema)
        if (!results.valid) {
            log.debug { "Could not validate vc against schema . The validation errors are:" }
            results.errors?.forEach {  log.debug { it }  }
        }
        return results.valid
    }

    override fun validateSchemaTsr(vc: String) = try {

        vc.toCredential().let {

            if (it is VerifiablePresentation) return true

            val credentialSchemaUrl = it.credentialSchema

            if (credentialSchemaUrl == null) {
                log.debug { "Credential has no associated credentialSchema property" }
                return false
            }

            val loadedSchema = try {
                URL(credentialSchemaUrl.id).readText()
            } catch (e: Exception) {
                if (log.isDebugEnabled) {
                    log.debug { "Could not load schema from ${credentialSchemaUrl.id}" }
                    e.printStackTrace()
                }
                return false
            }

            return validateSchema(it, loadedSchema)
        }
    } catch (e: Exception) {
        if (log.isDebugEnabled) {
            log.debug { "Could not validate schema" }
            e.printStackTrace()
        }
        false
    }
}
