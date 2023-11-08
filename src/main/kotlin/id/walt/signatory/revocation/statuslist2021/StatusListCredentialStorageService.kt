package id.walt.signatory.revocation.statuslist2021

import id.walt.common.resolveContent
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CCredentialSubject
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.templates.VcTemplateService
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.WaltIdServices
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.pathString

open class StatusListCredentialStorageService : WaltIdService() {
    override val implementation get() = serviceImplementation<StatusListCredentialStorageService>()

    open fun fetch(url: String): VerifiableCredential? = implementation.fetch(url)
    open fun store(issuer: String, id: String, purpose: String, bitString: String): Unit =
        implementation.store(issuer, id, purpose, bitString)

    companion object : ServiceProvider {
        override fun getService() = object : StatusListCredentialStorageService() {}
        override fun defaultImplementation() = WaltIdStatusListCredentialStorageService()
    }
}


class WaltIdStatusListCredentialStorageService : StatusListCredentialStorageService() {
    private val templateId = "StatusList2021Credential"
    private val signatoryService = Signatory.getService()
    private val templateService = VcTemplateService.getService()

    override fun fetch(url: String): VerifiableCredential? = let {
        val path = getCredentialPath(url)
        resolveContent(path).takeIf { it != path }?.let {
            VerifiableCredential.fromJson(it)
        }
    }

    override fun store(issuer: String, id: String, purpose: String, bitString: String): Unit = let {
        fetch(id)?.let { vc ->
            // update vc
            W3CCredentialSubject(
                vc.id, mapOf(
                    "type" to vc.credentialSubject!!.properties["type"] as String,
                    "statusPurpose" to vc.credentialSubject!!.properties["statusPurpose"] as String,
                    "encodedList" to bitString
                )
            )
            // new vc
        } ?: W3CCredentialSubject(
            id, mapOf(
                "type" to "StatusList2021Credential",
                "statusPurpose" to purpose,
                "encodedList" to bitString
            )
        )
    }.let {
        W3CCredentialBuilder.fromPartial(templateService.getTemplate(templateId).template!!).apply {
            setId(it.id ?: id)
            buildSubject {
                setFromJson(it.toJson())
            }
        }
    }.run {
        val credential = signatoryService.issue(
            credentialBuilder = this, config = ProofConfig(
                credentialId = id,
                issuerDid = issuer,
                subjectDid = issuer,
                proofType = ProofType.LD_PROOF,
            )
        ).toVerifiableCredential()
        getCredentialPath(credential.id!!).let {
            File(it).writeText(credential.encode())
        }
    }

    private fun getCredentialPath(name: String) =
        Path(WaltIdServices.revocationDir, "${URLEncoder.encode(name, StandardCharsets.UTF_8)}.cred").pathString
}
