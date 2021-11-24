package id.walt.model

class DidKey(
  context: List<String>,
  id: String,
  verificationMethod: List<VerificationMethod>? = null,
  authentication: List<String>? = null,
  assertionMethod: List<String>? = null,
  capabilityDelegation: List<String>? = null,
  capabilityInvocation: List<String>? = null,
  keyAgreement: List<String>? = null,
  serviceEndpoint: List<VerificationMethod>? = null
) : Did(
  context,
  id,
  verificationMethod,
  authentication,
  assertionMethod,
  capabilityDelegation,
  capabilityInvocation,
  keyAgreement,
  serviceEndpoint
) {
  constructor(context: String,
              id: String,
              verificationMethod: List<VerificationMethod>? = null,
              authentication: List<String>? = null,
              assertionMethod: List<String>? = null,
              capabilityDelegation: List<String>? = null,
              capabilityInvocation: List<String>? = null,
              keyAgreement: List<String>? = null,
              serviceEndpoint: List<VerificationMethod>? = null) : this(listOf(context), id, verificationMethod, authentication, assertionMethod, capabilityDelegation, capabilityInvocation, keyAgreement, serviceEndpoint)
}