package id.walt.services.vc

import com.nimbusds.jwt.SignedJWT
import deltadao.GaiaxCredential
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.*
import mu.KotlinLogging

private val log = KotlinLogging.logger("SSIKIT VcUtils")

object VcUtils {

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
        is GaiaxCredential -> vcObj.issuer
        else -> {
            log.warn { "No getIssuer for ${vcObj.type.last()}!" }
            ""
        }
    }

    fun getHolder(vcObj: VerifiableCredential): String = when (vcObj) {
        is Europass -> vcObj.credentialSubject!!.id!!
        is VerifiableId -> vcObj.credentialSubject!!.id!!
        is VerifiableDiploma -> vcObj.credentialSubject!!.id!!
        is PermanentResidentCard -> vcObj.credentialSubject!!.id!!
        is UniversityDegree -> vcObj.credentialSubject.id
        is VerifiableAttestation -> vcObj.credentialSubject!!.id
        is VerifiableAuthorization -> vcObj.credentialSubject.id
        is GaiaxCredential -> vcObj.credentialSubject.id
        else -> {
            log.warn { "No getHolder for ${vcObj.type.last()}!" }
            ""
        }
    }

}
