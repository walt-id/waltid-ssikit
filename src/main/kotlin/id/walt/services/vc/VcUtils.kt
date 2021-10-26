package id.walt.services.vc

import com.nimbusds.jwt.SignedJWT
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.*
import java.text.SimpleDateFormat
import java.util.*

object VcUtils {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    fun getIssuer(vcObj: VerifiableCredential): String = when (vcObj) {
        is Europass -> vcObj.issuer!!
        is VerifiableId -> vcObj.issuer!!
        is VerifiableDiploma -> vcObj.issuer!!
        is PermanentResidentCard -> vcObj.issuer!!
        is UniversityDegree -> vcObj.issuer.id
        is VerifiableAttestation -> vcObj.issuer
        is VerifiableAuthorization -> vcObj.issuer
        is VerifiablePresentation -> when (vcObj.jwt) {
            null -> vcObj.proof!!.creator!!
            else -> SignedJWT.parse(vcObj.jwt).jwtClaimsSet.issuer
        }
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

    fun getIssuanceDate(vc: VerifiableCredential): Date? {
        val dateString = when (vc) {
            is Europass -> vc.issuanceDate
            is VerifiableId -> vc.issuanceDate
            is VerifiableDiploma -> vc.issuanceDate
            is UniversityDegree -> vc.issuanceDate
            is VerifiableAttestation -> vc.issuanceDate
            is VerifiableAuthorization -> vc.issuanceDate
            else -> ""
        }
        return try { dateFormatter.parse(dateString) } catch (e: Exception) { null }
    }

    fun getValidFrom(vc: VerifiableCredential): Date? {
        val dateString = when (vc) {
            is Europass -> vc.validFrom
            is VerifiableId -> vc.validFrom
            is VerifiableDiploma -> vc.validFrom
            is VerifiableAttestation -> vc.validFrom
            is VerifiableAuthorization -> vc.validFrom
            else -> ""
        }
        return try { dateFormatter.parse(dateString) } catch (e: Exception) { null }
    }
}
