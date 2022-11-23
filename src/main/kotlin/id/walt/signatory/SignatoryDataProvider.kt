package id.walt.signatory

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import kotlin.reflect.KClass

interface SignatoryDataProvider {
    fun populate(credentialBuilder: W3CCredentialBuilder, proofConfig: ProofConfig): W3CCredentialBuilder
}
