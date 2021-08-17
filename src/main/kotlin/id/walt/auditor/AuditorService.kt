package id.walt.auditor

import id.walt.servicematrix.ServiceMatrix
import id.walt.services.vc.JsonLdCredentialService
import id.walt.vclib.Helpers.encode
import id.walt.vclib.VcLibManager
import id.walt.vclib.vclist.VerifiablePresentation

// the following validation policies can be applied
// - SIGNATURE
// - JSON_SCHEMA
// - TRUSTED_ISSUER_DID
// - TRUSTED_SUBJECT_DID
// - REVOCATION_STATUS
// - ISSUANCE_DATA_AFTER
// - EXPIRATION_DATE_BEFORE
// - REVOCATION_STATUS
// - SECURE_CRYPTO
// - HOLDER_BINDING (only for VPs)


interface VerificationPolicy {
    fun id(): String = this.javaClass.simpleName
    fun verify(vp: VerifiablePresentation): Boolean
}

class SignaturePolicy : VerificationPolicy {
    private val jsonLdCredentialService = JsonLdCredentialService.getService()

    override fun verify(vp: VerifiablePresentation) = jsonLdCredentialService.verifyVp(vp.encode())
}

class JsonSchemaPolicy : VerificationPolicy { // Schema already validated by json-ld?
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

class TrustedIssuerDidPolicy : VerificationPolicy {
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

class TrustedSubjectDidPolicy : VerificationPolicy {
    override fun verify(vp: VerifiablePresentation) = true // TODO validate policy
}

object PolicyRegistry {
    val policies = HashMap<String, VerificationPolicy>()

    fun register(policy: VerificationPolicy) = policies.put(policy.id(), policy)
    fun getPolicy(id: String) = policies[id]!!
}

data class VerificationResult(
    val overallStatus: Boolean = false,
    val policyResults: Map<VerificationPolicy, Boolean>
)

interface IAuditor {

    fun verify(vpJson: String, policies: List<VerificationPolicy>): VerificationResult
    fun verifyByIds(vpJson: String, policies: List<String>): VerificationResult =
        verify(vpJson, policies.map { PolicyRegistry.getPolicy(it) })

//    fun verifyVc(vc: String, config: AuditorConfig) = VerificationStatus(true)
//    fun verifyVp(vp: String, config: AuditorConfig) = VerificationStatus(true)
}

object AuditorService : IAuditor {

    private fun allAccepted(policyResults: Map<VerificationPolicy, Boolean>) = policyResults.values.all { it }


    override fun verify(vpJson: String, policies: List<VerificationPolicy>): VerificationResult {
        val vp = VcLibManager.getVerifiableCredential(vpJson) as VerifiablePresentation

        val policyResults = policies.associateWith { it.verify(vp) }

        return VerificationResult(allAccepted(policyResults), policyResults)
    }
}

fun main() {
    ServiceMatrix("service-matrix.properties")

    PolicyRegistry.register(SignaturePolicy())
    PolicyRegistry.register(TrustedIssuerDidPolicy())
    PolicyRegistry.register(TrustedSubjectDidPolicy())
    PolicyRegistry.register(JsonSchemaPolicy())

    val res = AuditorService.verify(
        """
      {
  "@context" : [ "https://www.w3.org/2018/credentials/v1" ],
  "id": "0",
  "type" : [ "VerifiableCredential", "VerifiablePresentation" ],
  "verifiableCredential" : [ {
    "@context" : [ "https://www.w3.org/2018/credentials/v1" ],
    "id" : "1626680615978",
    "type" : [ "VerifiableCredential", "VerifiableAttestation", "Europass" ],
    "issuer" : "did:ebsi:22RgRjvk6mVkUaZohDtDSweQFfMXTh1YBnCZu3JgPNEmx7zZ",
    "issuanceDate" : "2021-07-19T09:43:36.214321688",
    "credentialSubject" : {
      "@id" : "did:key:z6MkuHT1Q4FTSC9tqaU3bt7dCGesYNzjNMfYX1YUvWKXLpod",
      "currentFamilyName" : "Skywalker",
      "currentGivenName" : "Lea",
      "dateOfBirth" : "2021-02-15",
      "mailBox" : {
        "@uri" : "mailto:leaskywalker@gmail.com"
      }
    },
    "learningAchievement" : {
      "@id" : "urn:epass:learningAchievement:1",
      "title" : {
        "text" : {
          "@content-type" : "text/plain",
          "@lang" : "en",
          "#text" : "Degree in Biology"
        }
      },
      "specifiedBy" : {
        "@idref" : "urn:epass:qualification:1"
      }
    },
    "learningSpecificationReferences" : {
      "qualification" : {
        "@id" : "urn:epass:qualification:1",
        "title" : {
          "text" : {
            "@content-type" : "text/plain",
            "@lang" : "en",
            "#text" : "Degree in Biology"
          }
        },
        "awardingOpportunities" : null,
        "eqfLevel" : {
          "@targetFrameworkUrl" : "http://data.europa.eu/snb/eqf/25831c2",
          "@targetNotation" : "eqf",
          "@uri" : "http://data.europa.eu/snb/eqf/7",
          "targetName" : {
            "text" : {
              "@content-type" : "text/plain",
              "@lang" : "en",
              "#text" : "Level 7"
            }
          }
        }
      }
    },
    "targetDescription" : {
      "text" : {
        "@content-type" : "text/plain",
        "@lang" : "en",
        "#text" : "Highly specialised knowledge, some of which is at the forefront of knowledge in a field of work or study, as the basis for original thinking and/or research <html:br></html:br>critical awareness of knowledge issues in a field and at the interface between different fields </html:div> <html:div xmlns:html=\"http://www.w3.org/1999/xhtml\" class=\"Skill\">specialised problem-solving skills required in research and/or innovation in order to develop new knowledge and procedures and to integrate knowledge from different fields</html:div> <html:div xmlns:html=\"http://www.w3.org/1999/xhtml\" class=\"Competence\">manage and transform work or study contexts that are complex, unpredictable and require new strategic approaches <html:br></html:br>take responsibility for contributing to professional knowledge and practice and/or for reviewing the strategic performance of teams"
      }
    },
    "targetFrameworkName" : {
      "text" : {
        "@content-type" : "text/plain",
        "@lang" : "en",
        "#text" : "European Qualifications Framework for lifelong learning - (2008/C 111/01)"
      }
    },
    "credentialStatus" : {
      "id" : "https://essif.europa.eu/status/43",
      "type" : "CredentialsStatusList2020"
    },
    "credentialSchema" : {
      "id" : "https://essif.europa.eu/tsr-123/verifiableattestation.json",
      "type" : "JsonSchemaValidator2018"
    },
    "evidence" : {
      "id" : "https://essif.europa.eu/evidence/f2aeec97-fc0d-42bf-8ca7-0548192d4231",
      "type" : [ "DocumentVerification, Assessment" ],
      "verifier" : "https:// essif.europa.eu /issuers/48",
      "evidenceDocument" : [ "Passport", "Assessment" ]
    },
    "proof" : {
      "type" : "EcdsaSecp256k1Signature2019",
      "created" : "2021-07-19T07:43:36Z",
      "creator" : "did:ebsi:22RgRjvk6mVkUaZohDtDSweQFfMXTh1YBnCZu3JgPNEmx7zZ",
      "jws" : "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFUzI1NksifQ..rlHLKG45ZXX8yRw2pynVs4H79ZM2BOq1WqF7M42OwCIOoJVi4V31eTJCNMnANsgzWrDOTsCUfj8EAt4fvEKylA"
    }
  } ],
  "proof" : {
    "type" : "Ed25519Signature2018",
    "creator" : "did:key:z6MkuHT1Q4FTSC9tqaU3bt7dCGesYNzjNMfYX1YUvWKXLpod",
    "created" : "2021-07-19T07:45:50Z",
    "jws" : "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..uXAYH1UQL3D5YUoJRYLFbNsM5ojL0LVAWTVD3BRVu5Sieu9N3ueYbfN6y6TAPLNkMT5Ogs6Nu9M0O2U2gJdUCQ"
  }
}
    """, listOf(SignaturePolicy(), JsonSchemaPolicy())
    )

    println(res)
}
