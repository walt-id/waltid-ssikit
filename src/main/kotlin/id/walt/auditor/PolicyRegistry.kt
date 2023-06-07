package id.walt.auditor

import com.beust.klaxon.JsonObject
import com.beust.klaxon.KlaxonException
import id.walt.auditor.dynamic.DynamicPolicy
import id.walt.auditor.dynamic.DynamicPolicyArg
import id.walt.common.deepMerge
import mu.KotlinLogging
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

private val log = KotlinLogging.logger {}

open class PolicyFactory<P : VerificationPolicy, A : Any>(
    val policyType: KClass<P>,
    val argType: KClass<A>?,
    val name: String,
    val description: String? = null,
    val optionalArgument: Boolean = false
) {
    open fun create(argument: Any? = null): P {
        try {
            return argType?.let {
                if (optionalArgument) {
                    argument?.let {
                        return policyType.primaryConstructor!!.call(it)
                    }
                } else {
                    return policyType.primaryConstructor!!.call(argument)
                }
            } ?: policyType.createInstance()
        } catch (e: KlaxonException) {
            throw IllegalArgumentException("Provided argument was of wrong type.", e)
        } catch (e: InvocationTargetException) {
            throw IllegalArgumentException("No argument was provided.", e)
        }
    }

    val requiredArgumentType = when (argType) {
        null -> "None"
        else -> argType.simpleName!!
    }
}

class DynamicPolicyFactory(
    val dynamicPolicyArg: DynamicPolicyArg,
    val immutable: Boolean = false,
    name: String,
    description: String?
) : PolicyFactory<DynamicPolicy, JsonObject>(
    DynamicPolicy::class,
    JsonObject::class,
    name,
    description
) {
    override fun create(argument: Any?): DynamicPolicy {
        val mergedInput = if (argument != null) {
            JsonObject(dynamicPolicyArg.input).deepMerge(argument as JsonObject)
        } else {
            dynamicPolicyArg.input
        }
        return super.create(
            DynamicPolicyArg(
                input = mergedInput,
                policy = dynamicPolicyArg.policy,
                dataPath = dynamicPolicyArg.dataPath,
                policyQuery = dynamicPolicyArg.policyQuery,
                name = name
            )
        )
    }
}

object PolicyRegistry {

    private val delegate = PolicyRegistryService.getService()

    val defaultPolicyId: String = delegate.defaultPolicyId

    fun <P : ParameterizedVerificationPolicy<A>, A : Any> register(
        policy: KClass<P>,
        argType: KClass<A>,
        description: String? = null,
        optionalArgument: Boolean = false
    ) =
        delegate.register(policy, argType, description, optionalArgument)

    fun <P : SimpleVerificationPolicy> register(policy: KClass<P>, description: String? = null) =
        delegate.register(policy, description)

    fun <A : Any> getPolicy(id: String, argument: A? = null) =
        delegate.getPolicy(id, argument)

    fun getPolicy(id: String) = delegate.getPolicy(id)
    fun contains(id: String) = delegate.contains(id)
    fun listPolicies() = delegate.listPolicies()
    fun listPolicyInfo() = delegate.listPolicyInfo()

    fun getPolicyWithJsonArg(id: String, argumentJson: JsonObject?): VerificationPolicy =
        delegate.getPolicyWithJsonArg(id, argumentJson)

    fun getPolicyWithJsonArg(id: String, argumentJson: String?): VerificationPolicy =
        delegate.getPolicyWithJsonArg(id, argumentJson)

    fun isMutable(name: String): Boolean =
        delegate.isMutable(name)

    fun createSavedPolicy(name: String, dynPolArg: DynamicPolicyArg, override: Boolean, download: Boolean): Boolean =
        delegate.createSavedPolicy(name, dynPolArg, override, download)

    fun deleteSavedPolicy(name: String): Boolean =
        delegate.deleteSavedPolicy(name)
}
