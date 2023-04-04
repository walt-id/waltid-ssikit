package id.walt.auditor

import com.fasterxml.jackson.annotation.JsonIgnore
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import io.ktor.client.plugins.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.util.*

private val log = KotlinLogging.logger {}

abstract class VerificationPolicy {
    open val id: String
        get() = this.javaClass.simpleName
    abstract val description: String
    protected abstract fun doVerify(vc: VerifiableCredential): VerificationPolicyResult
    open val applyToVC = true
    open val applyToVP = true

    fun verify(vc: VerifiableCredential): VerificationPolicyResult {
        val verifyPresentation = vc is VerifiablePresentation && applyToVP
        val verifyCredential = vc !is VerifiablePresentation && applyToVC
        return when {
            verifyPresentation || verifyCredential -> doVerify(vc)
            else -> VerificationPolicyResult.success()
        }.also { log.debug { "VC ${vc.type} passes policy $id: $it" } }
    }
}

abstract class SimpleVerificationPolicy : VerificationPolicy()

abstract class ParameterizedVerificationPolicy<T>(val argument: T) : VerificationPolicy()

abstract class OptionalParameterizedVerificationPolicy<T>(argument: T?) : ParameterizedVerificationPolicy<T?>(argument)

data class VerificationResult(
    /***
     * Validation status over all policy results.
     */
    val result: Boolean = false,
    val policyResults: Map<String, VerificationPolicyResult>
) {
    @Deprecated("Deprecated in favour of: result")
    val valid: Boolean = result

    override fun toString() =
        "VerificationResult(result=$result, policyResults={${policyResults.entries.joinToString { it.key + "=" + it.value }}})"
}

@Serializable
data class VerificationPolicyMetadata(
    val id: String,
    val description: String?,
    val argumentType: String,
    val isMutable: Boolean
)

data class VerificationPolicyResult(val result: Boolean, @JsonIgnore val errors: List<Throwable> = listOf()) {
    companion object {
        fun success() = VerificationPolicyResult(true)
        fun failure(error: Throwable): VerificationPolicyResult {
            log.debug { "VerificationPolicy failed: ${error.stackTraceToString()}" }
            return VerificationPolicyResult(false, listOf(error))
        }

        fun failure(errors: List<Throwable> = listOf()) = VerificationPolicyResult(false, errors.toList())
    }

    val isSuccess = result
    val isFailure = !result

    fun getErrorString() = errors.mapIndexed { index, throwable ->
        "#${index + 1}: ${throwable::class.simpleName ?: "Error"} - ${throwable.message}"
    }.joinToString()

    override fun toString(): String {
        return when (result) {
            true -> "passed"
            false -> "failed: ${getErrorString()}"
        }
    }
}
