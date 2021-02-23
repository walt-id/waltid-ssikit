package org.letstrust

import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.crypto.provider.Ed25519Provider
import info.weboftrust.ldsignatures.crypto.provider.impl.TinkEd25519Provider
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.signer.EcdsaSecp256k1Signature2019LdSigner
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.verifier.EcdsaSecp256k1Signature2019LdVerifier
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2018LdVerifier
import org.bitcoinj.core.ECKey
import org.json.JSONObject
import java.net.URI
import java.util.*

object CredentialService {

    // Supported signatures
    enum class SignatureType {
        Ed25519Signature2018,
        EcdsaSecp256k1Signature2019,
        Ed25519Signature2020LdSigner
    }

    val kms = KeyManagementService

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

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val issuerKeys = KeyManagementService.loadKeys(issuerDid)

        val signer = when (signatureType) {
            SignatureType.Ed25519Signature2018 -> Ed25519Signature2018LdSigner(issuerKeys!!.getPrivateAndPublicKey())
            SignatureType.EcdsaSecp256k1Signature2019 -> EcdsaSecp256k1Signature2019LdSigner(
                ECKey.fromPrivate(issuerKeys!!.pair.private.encoded)
            )
            else -> throw Exception("Signature type $signatureType not supported")
        }
        // var signer = Ed25519Signature2018LdSigner(issuerKeys!!.getPrivateAndPublicKey())
        // following is working in version 0.4
        // var signer = Ed25519Signature2020LdSigner(issuerKeys!!.getPrivateAndPublicKey())

        signer.creator = URI.create(issuerDid)
        signer.created = Date() // Use the current date
        signer.domain = domain
        signer.nonce = nonce
        val proof = signer.sign(jsonLdObject)
        // println("proof")
        // println(proof)
        return jsonLdObject.toJson(true)
    }

    fun addProof(credMap: Map<String, String>, ldProof: LdProof): String {
        val signedCredMap = HashMap<String, Any>(credMap)
        signedCredMap["proof"] = JSONObject(ldProof.toJson())
        return JSONObject(signedCredMap).toString()
    }

    fun verify(issuerDid: String, vc: String, signatureType: SignatureType): Boolean {
        val jsonLdObject = JsonLDObject.fromJson(vc)

        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader

        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = true
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val issuerKeys = KeyManagementService.loadKeys(issuerDid)

        val verifier = when (signatureType) {
            SignatureType.Ed25519Signature2018 -> Ed25519Signature2018LdVerifier(issuerKeys!!.pair.public.encoded)
            SignatureType.EcdsaSecp256k1Signature2019 -> EcdsaSecp256k1Signature2019LdVerifier(
                ECKey.fromPublicOnly(
                    issuerKeys!!.pair.public.encoded
                )
            )
            else -> throw Exception("Signature type $signatureType not supported")
        }
        // following is working in version 0.4
        // val verifier = Ed25519Signature2020LdVerifier(issuerKeys!!.publicKey)
        return verifier.verify(jsonLdObject)
    }
}
