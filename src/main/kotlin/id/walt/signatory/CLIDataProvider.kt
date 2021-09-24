package id.walt.signatory

import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiableDiploma
import java.util.*

object CLIDataProviders {
    fun getCLIDataProviderFor(templateId: String): SignatoryDataProvider? {
        return when(templateId) {
            "VerifiableDiploma" -> VerifiableDiplomaCLIDataProvider()
            else -> null
        }
    }
}

abstract class CLIDataProvider : SignatoryDataProvider {
    fun prompt(prompt: String, default: String?): String? {
        System.out.print("$prompt [$default]: ")
        val input = readLine()
        return when(input.isNullOrBlank()) {
            true -> default
            else -> input
        }
    }

    fun promptInt(prompt: String, default: Int?): Int? {
        val str = prompt(prompt, default.let { it.toString() })
        return str.let { Integer.parseInt(it) }
    }
}

class VerifiableDiplomaCLIDataProvider : CLIDataProvider() {
    override fun populate(vc: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        vc as VerifiableDiploma
        vc.id = proofConfig.id ?: "education#higherEducation#${UUID.randomUUID()}"
        vc.issuer = proofConfig.issuerDid
        if (proofConfig.issueDate != null) vc.issuanceDate = dateFormat.format(proofConfig.issueDate)
        if (proofConfig.expirationDate != null) vc.expirationDate = dateFormat.format(proofConfig.expirationDate)
        vc.validFrom = vc.issuanceDate
        vc.credentialSubject!!.id = proofConfig.subjectDid
        vc.credentialSubject!!.awardingOpportunity!!.awardingBody.id = proofConfig.issuerDid

        println()
        println("Subject personal data, ID: ${proofConfig.subjectDid}")
        println("----------------------")
        vc.credentialSubject!!.identifier = prompt("Identifier", vc.credentialSubject!!.identifier)
        vc.credentialSubject!!.familyName = prompt("Family name", vc.credentialSubject!!.familyName)
        vc.credentialSubject!!.givenNames = prompt("Given names", vc.credentialSubject!!.givenNames)
        vc.credentialSubject!!.dateOfBirth = prompt("Date of birth", vc.credentialSubject!!.dateOfBirth)

        println()
        println("Awarding Opportunity")
        println("----------------------")
        vc.credentialSubject!!.awardingOpportunity!!.id = prompt("Opportunity ID", vc.credentialSubject!!.awardingOpportunity!!.id) ?: ""
        vc.credentialSubject!!.awardingOpportunity!!.identifier = prompt("Identifier", vc.credentialSubject!!.awardingOpportunity!!.identifier) ?: ""
        vc.credentialSubject!!.awardingOpportunity!!.location = prompt("Location", vc.credentialSubject!!.awardingOpportunity!!.location) ?: ""
        vc.credentialSubject!!.awardingOpportunity!!.startedAtTime = prompt("Started at", vc.credentialSubject!!.awardingOpportunity!!.startedAtTime) ?: ""
        vc.credentialSubject!!.awardingOpportunity!!.endedAtTime = prompt("Ended at", vc.credentialSubject!!.awardingOpportunity!!.endedAtTime) ?: ""

        println()
        println("Awarding Body, ID: ${proofConfig.issuerDid}")
        vc.credentialSubject!!.awardingOpportunity!!.awardingBody.preferredName = prompt("Preferred name", vc.credentialSubject!!.awardingOpportunity!!.awardingBody.preferredName) ?: ""
        vc.credentialSubject!!.awardingOpportunity!!.awardingBody.homepage = prompt("Homepage", vc.credentialSubject!!.awardingOpportunity!!.awardingBody.homepage) ?: ""
        vc.credentialSubject!!.awardingOpportunity!!.awardingBody.registration = prompt("Registration", vc.credentialSubject!!.awardingOpportunity!!.awardingBody.registration) ?: ""

        println()
        println("Grading scheme")
        println("----------------------")
        vc.credentialSubject!!.gradingScheme?.id = prompt("Grading Scheme ID", vc.credentialSubject!!.gradingScheme?.id) ?: ""
        vc.credentialSubject!!.gradingScheme?.title = prompt("Title", vc.credentialSubject!!.gradingScheme?.title) ?: ""

        println()
        println("Learning Achievement")
        println("----------------------")
        vc.credentialSubject!!.learningAchievement?.id = prompt("Learning achievement ID", vc.credentialSubject!!.learningAchievement?.id) ?: ""
        vc.credentialSubject!!.learningAchievement?.title = prompt("Title", vc.credentialSubject!!.learningAchievement?.title) ?: ""
        vc.credentialSubject!!.learningAchievement?.description = prompt("Description", vc.credentialSubject!!.learningAchievement?.description) ?: ""
        vc.credentialSubject!!.learningAchievement?.additionalNote = listOf(prompt("Additional note", vc.credentialSubject!!.learningAchievement?.additionalNote?.get(0)) ?: "")

        println()
        println("Learning Specification")
        println("----------------------")
        vc.credentialSubject!!.learningSpecification?.id = prompt("Learning specification ID", vc.credentialSubject!!.learningSpecification?.id) ?: ""
        vc.credentialSubject!!.learningSpecification?.ectsCreditPoints = promptInt("ECTS credit points", vc.credentialSubject!!.learningSpecification?.ectsCreditPoints)
        vc.credentialSubject!!.learningSpecification?.eqfLevel = promptInt("EQF Level", vc.credentialSubject!!.learningSpecification?.eqfLevel)
        vc.credentialSubject!!.learningSpecification?.iscedfCode = listOf(prompt("ISCEDF Code", vc.credentialSubject!!.learningSpecification?.iscedfCode?.get(0)) ?: "")
        vc.credentialSubject!!.learningSpecification?.nqfLevel = listOf(prompt("NQF Level", vc.credentialSubject!!.learningSpecification?.nqfLevel?.get(0)) ?: "")
        return vc
    }

}