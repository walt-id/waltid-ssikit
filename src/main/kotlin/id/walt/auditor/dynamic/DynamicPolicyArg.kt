package id.walt.auditor.dynamic

data class DynamicPolicyArg(
    val name: String = "DynamicPolicy",
    val description: String? = null,
    val input: Map<String, Any?>,
    val policy: String,
    val dataPath: String = "\$", // for specifying the credential data, default: the whole credential object
    val policyQuery: String = "data.system.main", // for evaluating the result from the rego engine
    val policyEngine: PolicyEngineType = PolicyEngineType.OPA,
    val applyToVC: Boolean = true,
    val applyToVP: Boolean = false
)
