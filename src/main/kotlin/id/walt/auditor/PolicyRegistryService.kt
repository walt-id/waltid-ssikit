package id.walt.auditor

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import id.walt.auditor.dynamic.DynamicPolicy
import id.walt.auditor.dynamic.DynamicPolicyArg
import id.walt.auditor.policies.*
import id.walt.common.resolveContent
import id.walt.model.dif.PresentationDefinition
import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.WaltIdService
import id.walt.services.context.ContextManager
import id.walt.services.hkvstore.HKVKey
import java.io.StringReader
import java.util.concurrent.atomic.*
import kotlin.reflect.KClass

open class PolicyRegistryService : WaltIdService() {
    override val implementation: PolicyRegistryService get() = serviceImplementation()

    companion object : ServiceProvider {
        const val SAVED_POLICY_ROOT_KEY = "policies"
        override fun getService() = ServiceRegistry.getService(PolicyRegistryService::class)
        override fun defaultImplementation() = PolicyRegistryService()
    }

    private val initialized = AtomicBoolean()
    private val _policies: MutableMap<String, PolicyFactory<*, *>> = mutableMapOf()
    private val policies get() = run {
        if (initialized.compareAndSet(false, true)) {
            initPolicies()
        }
        _policies
    }

    val defaultPolicyId: String = SignaturePolicy::class.simpleName!!

    fun <P : ParameterizedVerificationPolicy<A>, A : Any> register(
        policy: KClass<P>,
        argType: KClass<A>,
        description: String? = null,
        optionalArgument: Boolean = false
    ) = policies.put(policy.simpleName!!, PolicyFactory(policy, argType, policy.simpleName!!, description, optionalArgument))

    fun <P : OptionalParameterizedVerificationPolicy<A>, A : Any> register(
        policy: KClass<P>,
        argType: KClass<A>,
        description: String? = null
    ) = policies.put(policy.simpleName!!, PolicyFactory(policy, argType, policy.simpleName!!, description, true))

    fun <P : SimpleVerificationPolicy> register(policy: KClass<P>, description: String? = null) =
        policies.put(policy.simpleName!!, PolicyFactory<P, Unit>(policy, null, policy.simpleName!!, description))

    private fun registerSavedPolicy(name: String, dynamicPolicyArg: DynamicPolicyArg, immutable: Boolean = false) =
        policies.put(
            name,
            DynamicPolicyFactory(dynamicPolicyArg, immutable, name = name, description = dynamicPolicyArg.description)
        )

    fun <A : Any> getPolicy(id: String, argument: A? = null) = policies[id]!!.create(argument)
    fun getPolicy(id: String) = getPolicy(id, null)
    fun contains(id: String) = policies.containsKey(id)
    fun listPolicies() = policies.keys
    fun listPolicyInfo() = policies.values.map { p ->
        VerificationPolicyMetadata(
            p.name,
            p.description,
            p.requiredArgumentType,
            isMutable(p.name)
        )
    }

    fun getPolicyWithJsonArg(id: String, argumentJson: JsonObject?): VerificationPolicy {
        val policyFactory = policies[id] ?: throw IllegalArgumentException("No policy exists with id: $id")
        val argument =
            policyFactory.argType?.let {
                argumentJson?.let {
                    if (policyFactory.argType == JsonObject::class) {
                        argumentJson
                    } else {
                        Klaxon().fromJsonObject(
                            argumentJson,
                            policyFactory.argType.java,
                            policyFactory.argType
                        )
                    }
                }
            }

        return policyFactory.create(argument)
    }

    fun getPolicyWithJsonArg(id: String, argumentJson: String?): VerificationPolicy {
        return getPolicyWithJsonArg(id, argumentJson?.let { Klaxon().parseJsonObject(StringReader(it)) })
    }

    fun isMutable(name: String): Boolean {
        val polF = policies[name] ?: return false
        return polF is DynamicPolicyFactory && !polF.immutable
    }

    fun createSavedPolicy(name: String, dynPolArg: DynamicPolicyArg, override: Boolean, download: Boolean): Boolean {
        if (!contains(name) || (isMutable(name) && override)) {
            val policyContent = when (download) {
                true -> resolveContent(dynPolArg.policy)
                false -> dynPolArg.policy
            }
            val dynPolArgMod = DynamicPolicyArg(
                name,
                dynPolArg.description,
                dynPolArg.input,
                policyContent,
                dynPolArg.dataPath,
                dynPolArg.policyQuery,
                dynPolArg.policyEngine,
                dynPolArg.applyToVC,
                dynPolArg.applyToVP
            )
            ContextManager.hkvStore.put(HKVKey(SAVED_POLICY_ROOT_KEY, name), Klaxon().toJsonString(dynPolArgMod))
            registerSavedPolicy(name, dynPolArgMod)
            return true
        }
        return false
    }

    fun deleteSavedPolicy(name: String): Boolean {
        if (isMutable(name)) {
            ContextManager.hkvStore.delete(HKVKey(SAVED_POLICY_ROOT_KEY, name))
            policies.remove(name)
            return true
        }
        return false
    }

    open fun initSavedPolicies() {
        ContextManager.hkvStore.listChildKeys(HKVKey(SAVED_POLICY_ROOT_KEY)).forEach {
            registerSavedPolicy(it.name, Klaxon().parse(ContextManager.hkvStore.getAsString(it)!!)!!)
        }
    }

    open fun initPolicies() {
        register(SignaturePolicy::class, "Verify by signature")
        register(JsonSchemaPolicy::class, JsonSchemaPolicyArg::class, "Verify by JSON schema")
        register(EbsiTrustedSchemaRegistryPolicy::class, "Verify by EBSI Trusted Schema Registry")
        register(EbsiTrustedIssuerDidPolicy::class, "Verify by trusted issuer did")
        register(
            EbsiTrustedIssuerRegistryPolicy::class,
            EbsiTrustedIssuerRegistryPolicyArg::class,
            "Verify by an EBSI Trusted Issuers Registry compliant api.",
            true
        )
        register(EbsiTrustedIssuerAccreditationPolicy::class, "Verify by issuer's authorized claims")
        register(EbsiTrustedSubjectDidPolicy::class, "Verify by trusted subject did")
        register(IssuedDateBeforePolicy::class, "Verify by issuance date")
        register(ValidFromBeforePolicy::class, "Verify by valid from")
        register(ExpirationDateAfterPolicy::class, "Verify by expiration date")
        // register(GaiaxTrustedPolicy::class, "Verify Gaiax trusted fields")
        register(GaiaxSDPolicy::class, "Verify Gaiax SD fields")
        register(ChallengePolicy::class, ChallengePolicyArg::class, "Verify challenge")
        register(
            PresentationDefinitionPolicy::class,
            PresentationDefinition::class,
            "Verify that verifiable presentation complies with presentation definition"
        )
        register(CredentialStatusPolicy::class, "Verify by credential status")
        register(DynamicPolicy::class, DynamicPolicyArg::class, "Verify credential by rego policy")
        register(MultiSignaturePolicy::class, "Verify embedded multiple signatures")

        // predefined, hardcoded rego policy specializations
        // VerifiableMandate policy as specialized rego policy
        registerSavedPolicy(
            "VerifiableMandatePolicy",
            DynamicPolicyArg(
                "VerifiableMandatePolicy",
                "Predefined policy for verifiable mandates",
                JsonObject(),
                "$.credentialSubject.policySchemaURI",
                "$.credentialSubject.holder",
                "data.system.main"
            ),
            immutable = true
        )

        // other saved (Rego) policies
        initSavedPolicies()

        // RegoPolicy(RegoPolicyArg(mapOf(), "")).argument.input
    }
}
