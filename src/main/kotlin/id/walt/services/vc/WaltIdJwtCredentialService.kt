package id.walt.services.vc

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.services.jwt.JwtService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.Helpers.toMap
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiablePresentation
import info.weboftrust.ldsignatures.LdProof
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

private val log = KotlinLogging.logger {}

open class WaltIdJwtCredentialService : JwtCredentialService() {

    private val jwtService = JwtService.getService()

    override fun sign(jsonCred: String, config: ProofConfig): String {
        log.debug { "Signing JWT object with config: $config" }
        val issuerDid = config.issuerDid
        val issueDate = config.issueDate ?: Date()
        val payload = JWTClaimsSet.Builder()
            .jwtID(config.id)
            .issuer(issuerDid)
            .subject(config.subjectDid)
            .issueTime(issueDate)
            .notBeforeTime(issueDate)
            .expirationTime(config.expirationDate)
            .claim("vc", jsonCred.toCredential().toMap())
            .build().toString()

        log.debug { "Signing: $payload" }
        return jwtService.sign(issuerDid, payload)
    }

    override fun verifyVc(issuerDid: String, vc: String): Boolean {
        log.debug { "Verifying vc: $vc with issuerDid: $issuerDid" }
        return SignedJWT.parse(vc).header.keyID == issuerDid && verifyVc(vc)
    }

    override fun addProof(credMap: Map<String, String>, ldProof: LdProof): String =
        TODO("Not implemented yet.")

    override fun verify(vcOrVp: String): VerificationResult =
        TODO("Not implemented yet.")

    override fun verifyVc(vc: String): Boolean {
        log.debug { "Verifying vc: $vc" }
        return JwtService.getService().verify(vc)
    }

    override fun verifyVp(vp: String): Boolean =
        TODO("Not implemented yet.")

    override fun present(vc: String): String {
        log.debug { "Creating a presentation for VC:\n$vc" }

        val vpReqStr = VerifiablePresentation(
            id = "id",
            verifiableCredential = listOf(vc.toCredential())
        ).encode()

        log.trace { "VP request:\n$vpReqStr" }

        val holderDid = VcUtils.getHolder(vc.toCredential())
        val proofConfig = ProofConfig(
            issuerDid = holderDid,
            subjectDid = holderDid,
            proofType = ProofType.JWT
        )
        val vp = sign(vpReqStr, proofConfig)

        log.debug { "VP created:$vp" }
        return vp
    }

    override fun listVCs(): List<String> =
        Files.walk(Path.of("data/vc/created"))
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".json") }
            .map { it.fileName.toString() }.toList()

    override fun defaultVcTemplate(): VerifiableCredential =
        TODO("Not implemented yet.")
}
