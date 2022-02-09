package id.walt.signatory.dataproviders

import id.walt.signatory.ProofConfig
import id.walt.signatory.SignatoryDataProvider
import id.walt.vclib.model.VerifiableCredential

abstract class AbstractDataProvider<V : VerifiableCredential>: SignatoryDataProvider {

    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        val populatedVc = populateCustomData(template as V, proofConfig)

        // set meta data: id, subject, issuance info, ...
        populatedVc.setMetaData(
            id = proofConfig.credentialId,
            issuer = proofConfig.issuerDid,
            subject = proofConfig.subjectDid,
            issued = proofConfig.issueDate,
            validFrom = proofConfig.validDate,
            expirationDate = proofConfig.expirationDate)

        return populatedVc
    }

    abstract fun populateCustomData(template: V, proofConfig: ProofConfig): V

}
