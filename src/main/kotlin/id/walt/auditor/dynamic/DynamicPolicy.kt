package id.walt.auditor.dynamic

import com.jayway.jsonpath.JsonPath
import id.walt.auditor.ParameterizedVerificationPolicy
import id.walt.credentials.w3c.VerifiableCredential
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

open class DynamicPolicy(dynPolArg: DynamicPolicyArg) : ParameterizedVerificationPolicy<DynamicPolicyArg>(dynPolArg) {
    override val id: String
        get() = argument.name
    override val description = "Verify credential by rego policy"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        val json = vc.toJson()
        // params: rego (string, URL, file, credential property), input (json string), data jsonpath (default: $.credentialSubject)
        val rego = if (argument.policy.startsWith("$")) {
            JsonPath.parse(json).read(argument.policy)
        } else {
            argument.policy
        }
        return PolicyEngine.get(argument.policyEngine).validate(
            input = argument.input,
            data = JsonPath.parse(json)?.read(argument.dataPath)!!,
            policy = rego,
            query = argument.policyQuery
        ).also {
            log.debug { "DYNAMIC POLICY CHECK: VC ${vc.type} passed $it: $it" }
            log.debug { "Policy: ${argument.policy}" }
        }
    }

    override val applyToVC: Boolean
        get() = argument.applyToVC

    override val applyToVP: Boolean
        get() = argument.applyToVP
}
