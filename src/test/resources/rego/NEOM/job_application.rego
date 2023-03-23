# input.parameter
# {
#   "diploma_required": true,
#   "job_requirements": {
#       "diploma": {
#            "achievement": "MASTERS LAW, ECONOMICS AND MANAGEMENT",
#            "minEqfLevel": 7
#       },
#       "qualification": {
#           "achievement": "Master of Science in Civil Engineering",
#           "minEqfLevel": 5
#       }
#   }
# }
# supported credentials: QualificationCertificate or UniversityDiploma from src/main/resources/vc-templates/NEOM
# example input parameters: FulltimeJobInput.json, InternshipInput.json from src/test/resources/rego/NEOM

package system

import future.keywords.if
import future.keywords.in

default main := false

# university diploma is required, applicant has university diploma and diploma achievements match with job requirements
# university diploma is not required, applicant has university diploma and diploma achievements match with job requirements
main if {
	applicant_has_university_diploma
	university_diploma_matches_job_requirements
}

# university diploma is not required, applicant has qualification certificate and qualification matches with job requirements
main if {
	not university_diploma_required
	applicant_has_qualification_certificate
	qualification_matches_job_requirements
}

university_diploma_required if input.parameter.diploma_required

applicant_has_university_diploma if "UniversityDiploma" == input.credentialData.type[i]

applicant_has_qualification_certificate if "QualificationCertificate" == input.credentialData.type[i]

university_diploma_matches_job_requirements if {
	input.parameter.job_requirements.diploma.achievement == input.credentialData.credentialSubject.learningAchievement.title
	input.parameter.job_requirements.diploma.minEqfLevel <= input.credentialData.credentialSubject.learningSpecification.eqfLevel
}

qualification_matches_job_requirements if {
	some achievement in input.credentialData.credentialSubject.achieved
	input.parameter.job_requirements.qualification.achievement == achievement.title

	some spec in achievement.specifiedBy
	input.parameter.job_requirements.qualification.minEqfLevel <= to_number(trim_prefix(spec.eqfLevel, "http://data.europa.eu/snb/eqf/"))
}


