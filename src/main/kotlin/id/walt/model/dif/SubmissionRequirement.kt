package id.walt.model.dif

data class SubmissionRequirement (
  val rule: SubmissionRequirementRule,
  val from: String? = null,
  val from_nested: List<SubmissionRequirement>? = null,
  val name: String? = null,
  val purpose: String? = null,
  val count: Int? = null,
  val min: Int? = null,
  val max: Int? = null
    )
