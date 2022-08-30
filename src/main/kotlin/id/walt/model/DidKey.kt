package id.walt.model

class DidKey(
  context: List<String>,
  id: String,
  verificationMethod: List<VerificationMethod>? = null,
  authentication: List<VerificationMethod>? = null,
  assertionMethod: List<VerificationMethod>? = null,
  capabilityDelegation: List<VerificationMethod>? = null,
  capabilityInvocation: List<VerificationMethod>? = null,
  keyAgreement: List<VerificationMethod>? = null,
  serviceEndpoint: List<ServiceEndpoint>? = null
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
              authentication: List<VerificationMethod>? = null,
              assertionMethod: List<VerificationMethod>? = null,
              capabilityDelegation: List<VerificationMethod>? = null,
              capabilityInvocation: List<VerificationMethod>? = null,
              keyAgreement: List<VerificationMethod>? = null,
              serviceEndpoint: List<ServiceEndpoint>? = null) : this(listOf(context), id, verificationMethod, authentication, assertionMethod, capabilityDelegation, capabilityInvocation, keyAgreement, serviceEndpoint)
}
