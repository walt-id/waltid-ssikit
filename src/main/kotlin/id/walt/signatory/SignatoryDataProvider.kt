package id.walt.signatory

import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.Europass
import id.walt.vclib.vclist.VerifiableDiploma
import id.walt.vclib.vclist.VerifiableId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass

interface SignatoryDataProvider {
    fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential
}

val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

class EuropassDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): Europass {
        val vc = template as Europass
        vc.id = proofConfig.credentialId
        vc.issuer = proofConfig.issuerDid
        vc.credentialSubject!!.id = proofConfig.subjectDid

        return vc
    }
}

class VerifiableIdDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableId {
        val vc = template as VerifiableId

        vc.id = proofConfig.credentialId ?: "identity#verifiableID#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.validDate != null) vc.validFrom = dateFormat.format(proofConfig.validDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.validFrom = vc.issuanceDate
        vc.credentialSubject!!.id = proofConfig.subjectDid
        vc.evidence!!.verifier = proofConfig.issuerDid

        return vc
    }
}

class VerifiableDiplomaDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableDiploma {
        val vc = template as VerifiableDiploma

        vc.id = proofConfig.credentialId ?: "education#higherEducation#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.validDate != null) vc.validFrom = dateFormat.format(proofConfig.validDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.credentialSubject!!.id = proofConfig.subjectDid
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
        register(Europass::class, EuropassDataProvider())
    }
}
