package id.walt.signatory

import id.walt.vclib.model.CredentialStatus
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.Europass
import id.walt.vclib.vclist.VerifiableID
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

interface SignatoryDataProvider {
    fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential
}

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

class EuropassDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): Europass {
        val vc = template as Europass

        // TODO populate template and return fully defined VerifiableCredential

        return when (proofConfig.proofType) {
            ProofType.LD_PROOF -> populateForLDProof(vc, proofConfig)
            ProofType.JWT -> populateForJWTProof(vc, proofConfig)
        }
    }

    private fun populateForLDProof(vc: Europass, proofConfig: ProofConfig): Europass {
        val id = proofConfig.id ?: "education#higherEducation#${UUID.randomUUID()}"
        vc.id = id
        vc.issuer = proofConfig.issuerDid
        vc.credentialStatus = CredentialStatus("https://essif.europa.eu/status/$id", "CredentialsStatusList2020")

        if (proofConfig.subjectDid != null)
            vc.credentialSubject!!.id = proofConfig.subjectDid

        if (proofConfig.issueDate != null)
            vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        else if (vc.issuanceDate == null)
            vc.issuanceDate = dateFormat.format(Date())

        if (proofConfig.expirationDate != null)
            vc.expirationDate = dateFormat.format(proofConfig.expirationDate)

        return vc
    }

    private fun populateForJWTProof(vc: Europass, proofConfig: ProofConfig): Europass {
        vc.id = null
        vc.issuer = null
        vc.credentialSubject!!.id = null
        vc.issuanceDate = null
        vc.expirationDate = null
        vc.credentialStatus =
            if (proofConfig.id == null) null
            else CredentialStatus("https://essif.europa.eu/status/${proofConfig.id}", "CredentialsStatusList2020")
        return vc
    }
}

class VerifiableIDDataProvider : SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableID {
        val vc = template as VerifiableID

        // TODO populate template and return fully defined VerifiableCredential

        return when (proofConfig.proofType) {
            ProofType.LD_PROOF -> populateForLDProof(vc, proofConfig)
            ProofType.JWT -> populateForJWTProof(vc)
        }
    }

    private fun populateForLDProof(vc: VerifiableID, proofConfig: ProofConfig): VerifiableID {
        vc.id = proofConfig.id ?: "identity#verifiableID#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid

        if (proofConfig.subjectDid != null)
            vc.credentialSubject!!.id = proofConfig.subjectDid

        if (proofConfig.issueDate != null)
            vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        else if (vc.issuanceDate == null)
            vc.issuanceDate = dateFormat.format(Date())

        if (proofConfig.expirationDate != null)
            vc.expirationDate = dateFormat.format(proofConfig.expirationDate)

        return vc
    }

    private fun populateForJWTProof(vc: VerifiableID): VerifiableID {
        vc.id = null
        vc.issuer = null
        vc.credentialSubject!!.id = null
        vc.issuanceDate = null
        vc.expirationDate = null
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
        register(Europass::class, EuropassDataProvider())
        register(VerifiableID::class, VerifiableIDDataProvider())
    }
}
