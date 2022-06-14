package id.walt.auditor.dynamic

interface PolicyEngine {

  fun validate(input: Map<String, Any?>, data: Map<String, Any?>, policy: String, query: String): Boolean
  val type: PolicyEngineType

  companion object {
    fun get(type: PolicyEngineType): PolicyEngine {
      return when(type) {
        PolicyEngineType.OPA -> OPAPolicyEngine
      }
    }
  }
}
