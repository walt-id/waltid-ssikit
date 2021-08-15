package id.walt.services.vc

import com.nimbusds.jwt.JWTClaimsSet
import id.walt.common.prettyPrint
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.SignatureType
import id.walt.services.jwt.JwtService
import id.walt.services.keystore.KeyStoreService
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.Helpers.toMap
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.Europass
import info.weboftrust.ldsignatures.LdProof
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

open class WaltIdJwtCredentialService : JwtCredentialService() {

    private var ks = KeyStoreService.getService()
    private val jwtService = JwtService.getService()

    companion object {
        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    override fun sign(
        issuerDid: String,
        jsonCred: String,
        domain: String?,
        nonce: String?,
        verificationMethod: String?,
        proofPurpose: String?
    ): String {
        log.debug { "Signing JWT object with: issuerDid ($issuerDid), domain ($domain), nonce ($nonce)" }
        val credential = jsonCred.toCredential()

        val type = ks.load(issuerDid).let {
            when (it.algorithm) {
                KeyAlgorithm.ECDSA_Secp256k1 -> SignatureType.EcdsaSecp256k1Signature2019.name
                KeyAlgorithm.EdDSA_Ed25519 -> SignatureType.Ed25519Signature2018.name
                // else -> throw Exception("Signature for key algorithm ${it.algorithm} not supported")
            }
        }

        val jws = when (credential) {
            is Europass -> sign(issuerDid, credential)
            else -> throw IllegalStateException("Template not supported yet.")
        }

        val created = simpleDateFormat.format(Date.from(Instant.now()))
        credential.proof = Proof(type, null, created, proofPurpose, verificationMethod, jws)
        return credential.encode().prettyPrint()
    }

    private fun sign(issuerDid: String, credential: Europass): String {
        val issueTime = credential.issuanceDate?.let { simpleDateFormat.parse(it) }
        val expirationTime = credential.expirationDate?.let { simpleDateFormat.parse(it) }
        val payload = JWTClaimsSet.Builder()
            .jwtID(credential.id)
            .issuer(credential.issuer)
            .subject(credential.credentialSubject?.id)
            .issueTime(issueTime)
            .notBeforeTime(issueTime)
            .expirationTime(expirationTime)
            .claim("vc", buildVcClaim(credential.encode().toCredential() as Europass))
            .build().toString()

        log.debug { "Signing: $payload" }
        return jwtService.sign(issuerDid, payload)
    }

    private fun buildVcClaim(credential: Europass) =
        credential.let {
            it.id = null
            it.issuer = null
            it.credentialSubject?.id = null
            it.issuanceDate = null
            it.expirationDate = null
            it
        }.toMap()

    override fun verifyVc(issuerDid: String, vc: String): Boolean =
        when (val credential = vc.toCredential()) {
            is Europass -> credential.issuer.equals(issuerDid) && verifyVc(vc)
            else -> throw IllegalStateException("Template not supported yet.")
        }

    override fun addProof(credMap: Map<String, String>, ldProof: LdProof): String =
        TODO("Not implemented yet.")

    override fun verify(vcOrVp: String): VerificationResult =
        TODO("Not implemented yet.")

    override fun verifyVc(vc: String): Boolean =
        when (val credential = vc.toCredential()) {
            is Europass -> jwtService.verify(credential.proof!!.jws!!)
            else -> throw IllegalStateException("Template not supported yet.")
        }

    override fun verifyVp(vp: String): Boolean =
        TODO("Not implemented yet.")

    override fun present(vc: String, domain: String?, challenge: String?): String =
        TODO("Not implemented yet.")

    override fun listVCs(): List<String> =
        Files.walk(Path.of("data/vc/created"))
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".json") }
            .map { it.fileName.toString() }.toList()

    override fun defaultVcTemplate(): VerifiableCredential =
        TODO("Not implemented yet.")
}
