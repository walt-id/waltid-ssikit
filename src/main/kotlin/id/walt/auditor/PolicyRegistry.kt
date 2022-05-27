package id.walt.auditor

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class PolicyFactory<P : VerificationPolicy>(val policyType: KClass<P>) {
    fun create(argument: Any?): P {
        return policyType.createInstance().apply { arguments = argument }
    }
}

object PolicyRegistry {
    private val policies = LinkedHashMap<String, PolicyFactory<*>>()
    val defaultPolicyId: String

    fun <P : VerificationPolicy> register(policy: KClass<P>) = policies.put(policy.simpleName!!, PolicyFactory(policy))
    fun getPolicy(id: String, argument: Any? = null) = policies[id]!!.create(argument)
    fun contains(id: String) = policies.containsKey(id)
    fun listPolicies() = policies.keys

    init {
        defaultPolicyId = SignaturePolicy::class.simpleName!!
        register(SignaturePolicy::class)
        register(JsonSchemaPolicy::class)
        register(TrustedSchemaRegistryPolicy::class)
        register(TrustedIssuerDidPolicy::class)
        register(TrustedIssuerRegistryPolicy::class)
        register(TrustedSubjectDidPolicy::class)
        register(IssuedDateBeforePolicy::class)
        register(ValidFromBeforePolicy::class)
        register(ExpirationDateAfterPolicy::class)
        register(GaiaxTrustedPolicy::class)
        register(GaiaxSDPolicy::class)
        register(ChallengePolicy::class)
        register(VpTokenClaimPolicy::class)
        register(CredentialStatusPolicy::class)
        register(VerifiableMandatePolicy::class)
    }
}
