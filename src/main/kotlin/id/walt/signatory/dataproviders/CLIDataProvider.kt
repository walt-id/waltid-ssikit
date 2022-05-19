package id.walt.signatory.dataproviders

import id.walt.signatory.ProofConfig
import id.walt.signatory.SignatoryDataProvider
import id.walt.vclib.credentials.VerifiableDiploma
import id.walt.vclib.credentials.VerifiableId
import id.walt.vclib.credentials.VerifiableMandate
import id.walt.vclib.credentials.VerifiableVaccinationCertificate
import id.walt.vclib.credentials.gaiax.DataSelfDescription
import id.walt.vclib.credentials.gaiax.GaiaxCredential
import id.walt.vclib.credentials.gaiax.ParticipantCredential
import id.walt.vclib.model.VerifiableCredential

fun prompt(prompt: String, default: String?): String? {
    print("$prompt [$default]: ")
    val input = readLine()
    return when (input.isNullOrBlank()) {
        true -> default
        else -> input
    }
}

fun promptInt(prompt: String, default: Int?): Int {
    val str = prompt(prompt, default.let { it.toString() })
    return str.let { Integer.parseInt(it) }
}

object CLIDataProvider : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        return when (template) {
            is VerifiableDiploma -> VerifiableDiplomaCliDataProvider
            is VerifiableId -> VerifiableIdCliDataProvider
            is GaiaxCredential -> GaiaxCliDataProvider
            is DataSelfDescription -> GaiaxSDProvider
            is VerifiableVaccinationCertificate -> VerifiableVaccinationCertificateCliDataProvider
            is ParticipantCredential -> ParticipantCredentialProvider
            is VerifiableMandate -> VerifiableMandateCliDataProvider
            else -> {
                println("No CLI data provider defined for the given credential type. Only default meta data will be populated.")
                DefaultDataProvider
            }
        }.populate(template, proofConfig)
    }
}

object VerifiableDiplomaCliDataProvider : AbstractDataProvider<VerifiableDiploma>() {
    override fun populateCustomData(template: VerifiableDiploma, proofConfig: ProofConfig): VerifiableDiploma {
        template.apply {

            credentialSubject!!.apply {
                println()
                println("Subject personal data, ID: ${proofConfig.subjectDid}")
                println("----------------------")
                identifier = prompt("Identifier", identifier)
                familyName = prompt("Family name", familyName)
                givenNames = prompt("Given names", givenNames)
                dateOfBirth = prompt("Date of birth", dateOfBirth)

                println()
                println("Awarding Opportunity")
                println("----------------------")
                awardingOpportunity!!.apply {
                    id = prompt("Opportunity ID", id) ?: ""
                    identifier = prompt("Identifier", identifier) ?: ""
                    location = prompt("Location", location) ?: ""
                    startedAtTime = prompt("Started at", startedAtTime) ?: ""
                    endedAtTime = prompt("Ended at", endedAtTime) ?: ""

                    println()
                    println("Awarding Body, ID: ${proofConfig.issuerDid}")
                    awardingBody.apply {
                        preferredName = prompt("Preferred name", preferredName) ?: ""
                        homepage = prompt("Homepage", homepage) ?: ""
                        registration = prompt("Registration", registration) ?: ""
                        eidasLegalIdentifier = prompt("EIDAS Legal Identifier", eidasLegalIdentifier) ?: ""
                    }
                }

                println()
                println("Grading scheme")
                println("----------------------")
                gradingScheme?.apply {
                    id = prompt("Grading Scheme ID", id) ?: ""
                    title = prompt("Title", title) ?: ""
                    description = prompt("Description", description) ?: ""
                }

                println()
                println("Learning Achievement")
                println("----------------------")
                learningAchievement?.apply {
                    id = prompt("Learning achievement ID", id) ?: ""
                    title = prompt("Title", title) ?: ""
                    description = prompt("Description", description) ?: ""
                    additionalNote = listOf(prompt("Additional note", additionalNote?.get(0)) ?: "")
                }

                println()
                println("Learning Specification")
                println("----------------------")
                learningSpecification?.apply {
                    id = prompt("Learning specification ID", id) ?: ""
                    ectsCreditPoints = promptInt("ECTS credit points", ectsCreditPoints)
                    eqfLevel = promptInt("EQF Level", eqfLevel)
                    iscedfCode = listOf(prompt("ISCEDF Code", iscedfCode[0]) ?: "")
                    nqfLevel = listOf(prompt("NQF Level", nqfLevel[0]) ?: "")
                }
            }

            evidence?.apply {
                id = prompt("Evidence ID", id) ?: ""
                type = listOf(prompt("Evidence type", type?.get(0)) ?: "")
                verifier = prompt("Verifier", verifier) ?: ""
                evidenceDocument = listOf(prompt("Evidence document", evidenceDocument?.get(0)) ?: "")
                subjectPresence = prompt("Subject presence", subjectPresence) ?: ""
                documentPresence = listOf(prompt("Document presence", documentPresence?.get(0)) ?: "")
            }
        }

        return template
    }
}

object VerifiableVaccinationCertificateCliDataProvider : AbstractDataProvider<VerifiableVaccinationCertificate>() {
    override fun populateCustomData(template: VerifiableVaccinationCertificate, proofConfig: ProofConfig): VerifiableVaccinationCertificate {
        template.apply {

            credentialSubject!!.apply {

                println()
                println("Subject personal data, ID: ${proofConfig.subjectDid}")
                println("----------------------")
                familyName = prompt("Family name", familyName)
                givenNames = prompt("Given names", givenNames)
                dateOfBirth = prompt("Date of birth", dateOfBirth)


            }

            evidence?.apply {
                id = prompt("Evidence ID", id) ?: ""
                type = listOf(prompt("Evidence type", type?.get(0)) ?: "")
                verifier = prompt("Verifier", verifier) ?: ""
                evidenceDocument = listOf(prompt("Evidence document", evidenceDocument?.get(0)) ?: "")
                subjectPresence = prompt("Subject presence", subjectPresence) ?: ""
                documentPresence = listOf(prompt("Document presence", documentPresence?.get(0)) ?: "")
            }
        }

        return template
    }
}

object GaiaxCliDataProvider : AbstractDataProvider<GaiaxCredential>() {
    override fun populateCustomData(template: GaiaxCredential, proofConfig: ProofConfig): GaiaxCredential {
        template.apply {
            println()
            println("> Subject information")
            println()
            credentialSubject?.apply {
                legallyBindingName = prompt("Legally binding name", "deltaDAO AG") ?: ""
                brandName = prompt("Brand name", "deltaDAO") ?: ""
                legalRegistrationNumber = prompt("Legal registration number", "HRB 170364") ?: ""
                corporateEmailAddress = prompt("Corporate email address", "contact@delta-dao.com") ?: ""
                individualContactLegal = prompt("Individual contact legal", "legal@delta-dao.com") ?: ""
                individualContactTechnical = prompt("Individual contact technical", "support@delta-dao.com") ?: ""
                legalForm = prompt("Legal form", "Stock Company") ?: ""
                jurisdiction = prompt("Jurisdiction", "Germany") ?: ""
                trustState = prompt("Trust state", "trusted") ?: ""

                println()
                println("Legally binding address")
                println("----------------------")
                legallyBindingAddress.apply {
                    streetAddress = prompt("Street address", "Geibelstr. 46B") ?: ""
                    postalCode = prompt("Postal code", "22303") ?: ""
                    locality = prompt("Locality", "Hamburg") ?: ""
                    countryName = prompt("Country", "Germany") ?: ""
                }

                println()
                println("Web address")
                println("----------------------")
                webAddress.apply {
                    url = prompt("Web address URL", "https://www.delta-dao.com/") ?: ""
                }
                DNSpublicKey = prompt(
                    "DNS Public Key",
                    "04:8B:CA:33:B1:A1:3A:69:E6:A2:1E:BE:CB:4E:DF:75:A9:70:8B:AA:51:83:AB:A1:B0:5A:35:20:3D:B4:29:09:AD:67:B4:12:19:3B:6A:B5:7C:12:3D:C4:CA:DD:A5:E0:DA:05:1E:5E:1A:4B:D1:F2:BA:8F:07:4D:C7:B6:AA:23:46"
                ) ?: ""

                println()
                println("Commercial register")
                println("----------------------")
                commercialRegister.apply {
                    organizationName = prompt("Organization name", "Amtsgericht Hamburg (-Mitte)") ?: ""
                    organizationUnit = prompt("Organization unit", "Registergericht") ?: ""
                    streetAddress = prompt("Street address", "Caffamacherreihe 20") ?: ""
                    postalCode = prompt("Postal code", "20355") ?: ""
                    locality = prompt("Locality", "Hamburg") ?: ""
                    countryName = prompt("Country name", "Germany") ?: ""
                }

                println()
                println("Ethereum address")
                println("----------------------")
                ethereumAddress.apply {
                    id = prompt("Id", "0x4C84a36fCDb7Bc750294A7f3B5ad5CA8F74C4A52") ?: ""
                }
            }
        }

        return template
    }
}

object GaiaxSDProvider : AbstractDataProvider<DataSelfDescription>() {
    override fun populateCustomData(template: DataSelfDescription, proofConfig: ProofConfig): DataSelfDescription {
        template.apply {
            println()
            println("> Subject information")
            println()
            credentialSubject?.apply {
                type = prompt("Type", "Service") ?: ""
                hasName = prompt("Name", "AIS") ?: ""
                description = prompt("Description", "AIS demonstrates machine learning application use case.") ?: ""
                hasVersion = prompt("Version", "0.1.0") ?: ""
                providedBy = prompt("Provided by", "GAIA-X") ?: ""
                hasMarketingImage =
                    prompt("Marketing Image", "https://www.data-infrastructure.eu/GAIAX/Redaktion/EN/Bilder/UseCases/ai-marketplace-for-product-development.jpg?__blob=normal")
                        ?: ""
                hasCertifications = listOf(prompt("Certifications", hasCertifications?.get(0)) ?: "")
                utilizes = listOf(prompt("Utilizes", utilizes?.get(0)) ?: "")
                dependsOn = listOf(prompt("Depends on", dependsOn?.get(0)) ?: "")
            }
        }

        return template
    }
}

object VerifiableIdCliDataProvider : AbstractDataProvider<VerifiableId>() {
    override fun populateCustomData(template: VerifiableId, proofConfig: ProofConfig): VerifiableId {
        println()
        println("Subject personal data, ID: ${proofConfig.subjectDid}")
        println("----------------------")
        template.credentialSubject!!.firstName = prompt("First name", template.credentialSubject!!.firstName)
        template.credentialSubject!!.familyName = prompt("Family name", template.credentialSubject!!.familyName)
        template.credentialSubject!!.dateOfBirth = prompt("Date of birth", template.credentialSubject!!.dateOfBirth)
        template.credentialSubject!!.gender = prompt("Gender", template.credentialSubject!!.gender)
        template.credentialSubject!!.placeOfBirth = prompt("Place of birth", template.credentialSubject!!.placeOfBirth)
        template.credentialSubject!!.currentAddress = prompt("Current address", template.credentialSubject!!.currentAddress!![0])?.let { listOf(it) }

        return template
    }
}

object ParticipantCredentialProvider : AbstractDataProvider<ParticipantCredential>() {
    override fun populateCustomData(template: ParticipantCredential, proofConfig: ProofConfig): ParticipantCredential {
        template.apply {
            println()
            println("> Subject information")
            println()
            credentialSubject?.apply {
                hasRegistrationNumber = prompt("Registration Number", hasRegistrationNumber) ?: ""
                hasLegallyBindingName = prompt("Legally Binding Name", hasLegallyBindingName) ?: ""
                hasJurisdiction = prompt("Jurisdiction", hasJurisdiction) ?: ""
                hasCountry = prompt("Country", hasCountry) ?: ""
                leiCode = prompt("LEI", leiCode) ?: ""
                ethereumAddress = prompt("Ethereum Address", ethereumAddress) ?: ""
                parentOrganisation = prompt("Parent-organisation", parentOrganisation) ?: ""
                subOrganisation = prompt("Sub-organisation", subOrganisation) ?: ""
                id = prompt("Subject ID", id) ?: ""
            }
        }

        return template
    }
}

object VerifiableMandateCliDataProvider : AbstractDataProvider<VerifiableMandate>() {
    override fun populateCustomData(template: VerifiableMandate, proofConfig: ProofConfig): VerifiableMandate {
        println()
        template.apply {
            println()
            println("> Grant")
            println()
            credentialSubject!!.holder!!.apply {
                id = prompt("ID of holder", "did:ebsi:ze2dC9GezTtVSzjHVMQzpkE")!!
                role = prompt("Role", "family")!!
                grant = prompt("Name", "apply_to_masters")!!
                constraints = mapOf("location" to prompt("Location", "Slovenia")!!)
            }
        }
        return template
    }
}