package id.walt.model.oidc

import com.nimbusds.jwt.JWTClaimsSet
import id.walt.common.KlaxonWithConverters
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.model.dif.PresentationSubmission
import id.walt.services.jwt.JwtService
import id.walt.services.oidc.OIDCUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

data class SIOPv2Response(
    val vp_token: List<VerifiablePresentation>,
    val presentation_submission: PresentationSubmission,
    val id_token: String?,
    val state: String?
) {

    private fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)

    fun toFormParams(): Map<String, String> {
        val vpTokenString = OIDCUtils.toVpToken(vp_token)
        return buildMap {
            put("vp_token", vpTokenString)
            put("presentation_submission", KlaxonWithConverters().toJsonString(presentation_submission))
            id_token?.let { put("id_token", it) }
            state?.let { put("state", it) }
        }
    }

    fun toFormBody(): String {
        return toFormParams().map { "${enc(it.key)}=${enc(it.value)}" }.joinToString("&")
    }

    fun toEBSIWctJson(): String {
        val idToken = SelfIssuedIDToken.parse(id_token!!)
        return KlaxonWithConverters().toJsonString(
            mapOf(
                "id_token" to id_token,
                "vp_token" to vp_token.flatMap { vp -> vp.verifiableCredential ?: listOf() }.map { vc ->
                    mapOf(
                        "format" to "jwt_vp", "presentation" to JwtService.getService().sign(
                            idToken!!.subject,
                            JWTClaimsSet.Builder().subject(idToken.subject).issuer(idToken.subject).issueTime(Date())
                                .claim("nonce", vp_token.first().challenge).jwtID(vc.id).claim("vc", vc.encode()).build()
                                .toString()
                        )
                    )
                })
        )
    }

    companion object {
        fun fromFormParams(params: Map<String, String>): SIOPv2Response {
            val vpTokenStr = params["vp_token"] ?: throw IllegalArgumentException("vp_token parameter must be set")
            val idToken = params["id_token"]
            val presentationSubmissionStr = params["presentation_submission"]
            return SIOPv2Response(
                vp_token = OIDCUtils.fromVpToken(vpTokenStr),
                presentation_submission = presentationSubmissionStr?.let { KlaxonWithConverters().parse<PresentationSubmission>(it) }
                    ?: idToken?.let { IDToken.parse(it)?.vpTokenRef?.presentation_submission }
                    ?: throw IllegalArgumentException("Could not parse presentation_submission parameter"),
                id_token = idToken,
                state = params["state"]
            )
        }
    }
}
