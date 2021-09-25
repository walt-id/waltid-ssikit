package id.walt.services.vc

import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.*

object VcUtils {

    fun getIssuer(vcObj: VerifiableCredential): String = when (vcObj) {
        is Europass -> vcObj.issuer!!
        is VerifiableId -> vcObj.issuer!!
        is VerifiableDiploma -> vcObj.issuer!!
        is PermanentResidentCard -> vcObj.issuer!!
        is UniversityDegree -> vcObj.issuer.id
        is VerifiableAttestation -> vcObj.issuer
        is VerifiableAuthorization -> vcObj.issuer
        is VerifiablePresentation -> vcObj.proof!!.creator!!
        else -> ""
    }

    fun getHolder(vcObj: VerifiableCredential): String = when (vcObj) {
        is Europass -> vcObj.credentialSubject!!.id!!
        is VerifiableId -> vcObj.credentialSubject!!.id!!
        is VerifiableDiploma -> vcObj.credentialSubject!!.id!!
        is PermanentResidentCard -> vcObj.credentialSubject!!.id!!
        is UniversityDegree -> vcObj.credentialSubject.id
        is VerifiableAttestation -> vcObj.credentialSubject!!.id
        is VerifiableAuthorization -> vcObj.credentialSubject.id
        else -> ""
    }

}
