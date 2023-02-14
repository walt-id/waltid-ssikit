package id.walt.services.oidc

import java.net.URI

/**
 * These hacks are required as you can't call
 * URI.create("openid://")
 */
object OidcSchemeFixer {

    val schemeReplacement = "waltid-authentication-request-uri-hack"

    val openIdSchemeFix = "openid://$schemeReplacement"
    val openIdSchemeFixUri: URI = URI.create(openIdSchemeFix)
    val openidInitiateIssuanceSchemeFix = "openid-initiate-issuance://$schemeReplacement"
    val openidInitiateIssuanceSchemeFixUri: URI = URI.create(openidInitiateIssuanceSchemeFix)

    fun String.safeOpenidScheme(): URI = URI.create(if (this == "openid://") openIdSchemeFix else this)
    fun String.safeOpenidInitiateIssuanceScheme(): URI =
        URI.create(if (this == "openid-initiate-issuance://") openidInitiateIssuanceSchemeFix else this)

    fun String.unescapeOpenIdScheme() = this.replace(schemeReplacement, "")
    fun URI.unescapeOpenIdScheme(): URI = URI.create(this.toString().unescapeOpenIdScheme())
}
