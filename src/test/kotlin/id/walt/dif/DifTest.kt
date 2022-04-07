package id.walt.dif

import id.walt.model.dif.PresentationDefinition
import id.walt.model.oidc.klaxon
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DifTest: AnnotationSpec() {

  @Test
  fun testParsePresentationDefinition() {
    val pd = "{\n" +
        "            \"id\": \"vp token example\",\n" +
        "            \"input_descriptors\": [\n" +
        "                {\n" +
        "                    \"id\": \"id card credential\",\n" +
        "                    \"format\": {\n" +
        "                        \"ldp_vc\": {\n" +
        "                            \"proof_type\": [\n" +
        "                                \"Ed25519Signature2018\"\n" +
        "                            ]\n" +
        "                        }\n" +
        "                    },\n" +
        "                    \"constraints\": {\n" +
        "                        \"fields\": [\n" +
        "                            {\n" +
        "                                \"path\": [\n" +
        "                                    \"\$.type\"\n" +
        "                                ],\n" +
        "                                \"filter\": {\n" +
        "                                    \"type\": \"string\",\n" +
        "                                    \"pattern\": \"VerifiableId\"\n" +
        "                                }\n" +
        "                            }\n" +
        "                        ]\n" +
        "                    }\n" +
        "                }\n" +
        "            ]\n" +
        "        }"

    val parsedPD = klaxon.parse<PresentationDefinition>(pd)
    parsedPD shouldNotBe null
    parsedPD!!.id shouldBe "vp token example"
    parsedPD!!.input_descriptors shouldNotBe null
    parsedPD!!.input_descriptors!!.size shouldBe 1
    parsedPD!!.input_descriptors[0].id shouldBe "id card credential"
    parsedPD!!.input_descriptors[0].format shouldNotBe null
  }

  @Test
  fun testParsePresentationDefinitionWithSubmissionRequirements() {
    val pd = "{\n" +
        "    \"id\": \"32f54163-7166-48f1-93d8-ff217bdb0653\",\n" +
        "    \"submission_requirements\": [\n" +
        "      {\n" +
        "        \"name\": \"Banking Information\",\n" +
        "        \"purpose\": \"We can only remit payment to a currently-valid bank account in the US, Germany or France.\",\n" +
        "        \"rule\": \"pick\",\n" +
        "        \"count\": 1,\n" +
        "        \"from\": \"A\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Employment Information\",\n" +
        "        \"purpose\": \"We are only verifying one current employment relationship, not any other information about employment.\",\n" +
        "        \"rule\": \"all\",\n" +
        "        \"from\": \"B\"\n" +
        "      },\n" +
        "      {\n" +
        "        \"name\": \"Eligibility to Drive on US Roads\",\n" +
        "        \"purpose\": \"We need to verify eligibility to drive on US roads via US or EU driver's license, but no biometric or identifying information contained there.\",\n" +
        "        \"rule\": \"pick\",\n" +
        "        \"count\": 1,\n" +
        "        \"from\": \"C\"\n" +
        "      }\n" +
        "    ],\n" +
        "    \"input_descriptors\": [\n" +
        "      {\n" +
        "        \"id\": \"banking_input_1\",\n" +
        "        \"name\": \"Bank Account Information\",\n" +
        "        \"purpose\": \"Bank Account required to remit payment.\",\n" +
        "        \"group\": [\"A\"],\n" +
        "        \"constraints\": {\n" +
        "          \"limit_disclosure\": \"required\",\n" +
        "          \"fields\": [\n" +
        "            {\n" +
        "              \"path\": [\"\$.credentialSchema\", \"\$.vc.credentialSchema\"],\n" +
        "              \"filter\": {\n" +
        "                \"allOf\": [\n" +
        "                  {\n" +
        "                    \"type\": \"array\",\n" +
        "                    \"contains\": {\n" +
        "                      \"type\": \"object\",\n" +
        "                      \"properties\": {\n" +
        "                        \"id\": {\n" +
        "                          \"type\": \"string\",\n" +
        "                          \"pattern\": \"https://bank-standards.example.com#accounts\"\n" +
        "                        }\n" +
        "                      },\n" +
        "                      \"required\": [\"id\"]\n" +
        "                    }\n" +
        "                  },\n" +
        "                  {\n" +
        "                    \"type\": \"array\",\n" +
        "                    \"contains\": {\n" +
        "                      \"type\": \"object\",\n" +
        "                      \"properties\": {\n" +
        "                        \"id\": {\n" +
        "                          \"type\": \"string\",\n" +
        "                          \"pattern\": \"https://bank-standards.example.com#investments\"\n" +
        "                        }\n" +
        "                      },\n" +
        "                      \"required\": [\"id\"]\n" +
        "                    }\n" +
        "                  }\n" +
        "                ]\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\"\$.issuer\", \"\$.vc.issuer\", \"\$.iss\"],\n" +
        "              \"purpose\": \"We can only verify bank accounts if they are attested by a trusted bank, auditor or regulatory authority.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"pattern\": \"did:example:123|did:example:456\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\n" +
        "                \"\$.credentialSubject.account[*].account_number\",\n" +
        "                \"\$.vc.credentialSubject.account[*].account_number\",\n" +
        "                \"\$.account[*].account_number\"\n" +
        "              ],\n" +
        "              \"purpose\": \"We can only remit payment to a currently-valid bank account in the US, France, or Germany, submitted as an ABA Acct # or IBAN.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"pattern\": \"^[0-9]{10-12}|^(DE|FR)[0-9]{2}\\\\s?([0-9a-zA-Z]{4}\\\\s?){4}[0-9a-zA-Z]{2}\$\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\"\$.credentialSubject.portfolio_value\", \"\$.vc.credentialSubject.portfolio_value\", \"\$.portfolio_value\"],\n" +
        "              \"purpose\": \"A current portfolio value of at least one million dollars is required to insure your application\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"number\",\n" +
        "                \"minimum\": 1000000\n" +
        "              }\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      },\n" +
        "      {\n" +
        "        \"id\": \"banking_input_2\",\n" +
        "        \"name\": \"Bank Account Information\",\n" +
        "        \"purpose\": \"We can only remit payment to a currently-valid bank account.\",\n" +
        "        \"group\": [\"A\"],\n" +
        "        \"constraints\": {\n" +
        "          \"fields\": [\n" +
        "            {\n" +
        "              \"path\": [\"\$.credentialSchema.id\", \"\$.vc.credentialSchema.id\"],\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"pattern\": \"https://bank-schemas.org/1.0.0/accounts.json|https://bank-schemas.org/2.0.0/accounts.json\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\n" +
        "                \"\$.issuer\",\n" +
        "                \"\$.vc.issuer\",\n" +
        "                \"\$.iss\"\n" +
        "              ],\n" +
        "              \"purpose\": \"We can only verify bank accounts if they are attested by a trusted bank, auditor or regulatory authority.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"pattern\": \"did:example:123|did:example:456\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\n" +
        "                \"\$.credentialSubject.account[*].id\",\n" +
        "                \"\$.vc.credentialSubject.account[*].id\",\n" +
        "                \"\$.account[*].id\"\n" +
        "              ],\n" +
        "              \"purpose\": \"We can only remit payment to a currently-valid bank account in the US, France, or Germany, submitted as an ABA Acct # or IBAN.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"pattern\": \"^[0-9]{10-12}|^(DE|FR)[0-9]{2}\\\\s?([0-9a-zA-Z]{4}\\\\s?){4}[0-9a-zA-Z]{2}\$\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\n" +
        "                \"\$.credentialSubject.account[*].route\",\n" +
        "                \"\$.vc.credentialSubject.account[*].route\",\n" +
        "                \"\$.account[*].route\"\n" +
        "              ],\n" +
        "              \"purpose\": \"We can only remit payment to a currently-valid account at a US, Japanese, or German federally-accredited bank, submitted as an ABA RTN or SWIFT code.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"pattern\": \"^[0-9]{9}|^([a-zA-Z]){4}([a-zA-Z]){2}([0-9a-zA-Z]){2}([0-9a-zA-Z]{3})?\$\"\n" +
        "              }\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      },\n" +
        "      {\n" +
        "        \"id\": \"employment_input\",\n" +
        "        \"name\": \"Employment History\",\n" +
        "        \"purpose\": \"We are only verifying one current employment relationship, not any other information about employment.\",\n" +
        "        \"group\": [\"B\"],\n" +
        "        \"constraints\": {\n" +
        "          \"limit_disclosure\": \"required\",\n" +
        "          \"fields\": [\n" +
        "            {\n" +
        "              \"path\": [\"\$.credentialSchema\", \"\$.vc.credentialSchema\"],\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"const\": \"https://business-standards.org/schemas/employment-history.json\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\"\$.jobs[*].active\"],\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"boolean\",\n" +
        "                \"pattern\": \"true\"\n" +
        "              }\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      },\n" +
        "      {\n" +
        "        \"id\": \"drivers_license_input_1\",\n" +
        "        \"name\": \"EU Driver's License\",\n" +
        "        \"group\": [\"C\"],\n" +
        "        \"constraints\": {\n" +
        "          \"fields\": [\n" +
        "            {\n" +
        "              \"path\": [\"\$.credentialSchema.id\", \"\$.vc.credentialSchema.id\"],\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"const\": \"https://schema.eu/claims/DriversLicense.json\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\"\$.issuer\", \"\$.vc.issuer\", \"\$.iss\"],\n" +
        "              \"purpose\": \"We can only accept digital driver's licenses issued by national authorities of EU member states or trusted notarial auditors.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"pattern\": \"did:example:gov1|did:example:gov2\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\"\$.credentialSubject.dob\", \"\$.vc.credentialSubject.dob\", \"\$.dob\"],\n" +
        "              \"purpose\": \"We must confirm that the driver was at least 21 years old on April 16, 2020.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"format\": \"date\",\n" +
        "                \"formatMaximum\": \"1999-05-16\"\n" +
        "              }\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      },\n" +
        "      {\n" +
        "        \"id\": \"drivers_license_input_2\",\n" +
        "        \"name\": \"Driver's License from one of 50 US States\",\n" +
        "        \"group\": [\"C\"],\n" +
        "        \"constraints\": {\n" +
        "          \"fields\": [\n" +
        "            {\n" +
        "              \"path\": [\"\$.credentialSchema.id\", \"\$.vc.credentialSchema.id\"],\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"const\": \"hub://did:foo:123/Collections/schema.us.gov/american_drivers_license.json\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\"\$.issuer\", \"\$.vc.issuer\", \"\$.iss\"],\n" +
        "              \"purpose\": \"We can only accept digital driver's licenses issued by the 50 US states' automative affairs agencies.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"pattern\": \"did:example:gov1|did:web:dmv.ca.gov|did:example:oregonDMV\"\n" +
        "              }\n" +
        "            },\n" +
        "            {\n" +
        "              \"path\": [\"\$.credentialSubject.birth_date\", \"\$.vc.credentialSubject.birth_date\", \"\$.birth_date\"],\n" +
        "              \"purpose\": \"We must confirm that the driver was at least 21 years old on April 16, 2020.\",\n" +
        "              \"filter\": {\n" +
        "                \"type\": \"string\",\n" +
        "                \"format\": \"date\",\n" +
        "                \"forrmatMaximum\": \"1999-05-16\"\n" +
        "              }\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      }\n" +
        "    ]\n" +
        "  }"

    val parsedPD = klaxon.parse<PresentationDefinition>(pd)
    parsedPD shouldNotBe null
  }
}
