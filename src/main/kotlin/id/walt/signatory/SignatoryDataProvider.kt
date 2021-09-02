package id.walt.signatory

import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiableDiploma
import id.walt.vclib.vclist.VerifiableId
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

interface SignatoryDataProvider {
    fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential
}

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

class VerifiableIdDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableId {
        val vc = template as VerifiableId
        return when (proofConfig.proofType) {
            ProofType.LD_PROOF -> populateForLDProof(vc, proofConfig)
            ProofType.JWT -> populateForJWTProof(vc, proofConfig)
        }
    }

    private fun populateForLDProof(vc: VerifiableId, proofConfig: ProofConfig): VerifiableId {
        vc.id = proofConfig.id ?: "identity#verifiableID#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.validFrom = vc.issuanceDate
        vc.credentialSubject!!.id = proofConfig.subjectDid
        vc.evidence!!.verifier = proofConfig.issuerDid
        return vc
    }

    private fun populateForJWTProof(vc: VerifiableId, proofConfig: ProofConfig): VerifiableId {
        vc.id = null; vc.issuer = null; vc.issuanceDate = null; vc.expirationDate; vc.credentialSubject!!.id = null
        if (proofConfig.issueDate != null) vc.validFrom = dateFormat.format(proofConfig.issueDate)
        vc.evidence!!.verifier = proofConfig.issuerDid
        return vc
    }
}

class VerifiableDiplomaDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableDiploma {
        val vc = template as VerifiableDiploma
        return when (proofConfig.proofType) {
            ProofType.LD_PROOF -> populateForLDProof(vc, proofConfig)
            ProofType.JWT -> populateForJWTProof(vc, proofConfig)
        }
    }

    private fun populateForLDProof(vc: VerifiableDiploma, proofConfig: ProofConfig): VerifiableDiploma {
        vc.id = proofConfig.id ?: "education#higherEducation#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.validFrom = vc.issuanceDate
        vc.credentialSubject!!.id = proofConfig.subjectDid
        vc.credentialSubject!!.awardingOpportunity!!.awardingBody.id = proofConfig.issuerDid
        return vc
    }

    private fun populateForJWTProof(vc: VerifiableDiploma, proofConfig: ProofConfig): VerifiableDiploma {
        vc.id = null; vc.issuer = null; vc.issuanceDate = null; vc.expirationDate; vc.credentialSubject!!.id = null
        if (proofConfig.issueDate != null) vc.validFrom = dateFormat.format(proofConfig.issueDate)
        vc.credentialSubject!!.awardingOpportunity!!.awardingBody.id = proofConfig.issuerDid
        return vc
    }
}

class NoSuchDataProviderException(credentialType: KClass<out VerifiableCredential>) :
    Exception("No data provider is registered for ${credentialType.simpleName}")

object DataProviderRegistry {
    val providers = HashMap<KClass<out VerifiableCredential>, SignatoryDataProvider>()

    // TODO register via unique stringId
    fun register(credentialType: KClass<out VerifiableCredential>, provider: SignatoryDataProvider) =
        providers.put(credentialType, provider)

    fun getProvider(credentialType: KClass<out VerifiableCredential>) =
        providers[credentialType] ?: throw NoSuchDataProviderException(credentialType)

    init {
        // Init default providers
        register(VerifiableDiploma::class, VerifiableDiplomaDataProvider())
        register(VerifiableId::class, VerifiableIdDataProvider())
    }
}
