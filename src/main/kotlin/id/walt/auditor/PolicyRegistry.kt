package id.walt.auditor

object PolicyRegistry {
    private val policies = LinkedHashMap<String, VerificationPolicy>()
    val defaultPolicyId: String

    fun register(policy: VerificationPolicy) = policies.put(policy.id, policy)
    fun getPolicy(id: String) = policies[id]!!
    fun contains(id: String) = policies.containsKey(id)
    fun listPolicies() = policies.values

    init {
        val sigPol = SignaturePolicy()
        defaultPolicyId = sigPol.id
        register(sigPol)
        register(JsonSchemaPolicy())
        register(TrustedSchemaRegistryPolicy())
        register(TrustedIssuerDidPolicy())
        register(TrustedIssuerRegistryPolicy())
        register(TrustedSubjectDidPolicy())
        register(IssuanceDateBeforePolicy())
        register(ValidFromBeforePolicy())
        register(ExpirationDateAfterPolicy())
        register(GaiaxTrustedPolicy())
        register(GaiaxSDPolicy())
        register(ChallengePolicy(""))
        register(VpTokenClaimPolicy(null))
    }
}
