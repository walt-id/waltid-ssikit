package id.walt.signatory.dataproviders

import id.walt.signatory.ProofConfig
import id.walt.vclib.model.VerifiableCredential

object DefaultDataProvider : AbstractDataProvider<VerifiableCredential>() {
    override fun populateCustomData(vc: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        // nothing to do
        return vc
    }
}
