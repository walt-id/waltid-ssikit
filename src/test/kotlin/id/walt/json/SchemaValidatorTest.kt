package id.walt.json

import id.walt.auditor.Auditor
import id.walt.auditor.TrustedSchemaRegistryPolicy
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.*
import id.walt.signatory.dataproviders.DefaultDataProvider
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.credentials.Europass
import id.walt.vclib.credentials.VerifiableDiploma
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.nio.file.Path
import java.util.*

class SchemaValidatorTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val did = DidService.create(DidMethod.key)

    "Validates 2019-09 schema with recursive refs" {
        mockkObject(DataProviderRegistry)
        every { DataProviderRegistry.getProvider(Europass::class) } returns RecursiveCredentialSchema201909DataProvider()
        val vc = Signatory.getService().issue("Europass", ProofConfig(
            issuerDid = did,
            subjectDid = did,
            proofType = ProofType.JWT,
            credentialId = "urn:uuid:${UUID.randomUUID()}"
        ))
        Auditor.getService().verify(vc, listOf(TrustedSchemaRegistryPolicy())).valid shouldBe true
        unmockkAll()
    }

    "Does not validate 2020-12 schema with recursive refs" {
        mockkObject(DataProviderRegistry)
        every { DataProviderRegistry.getProvider(Europass::class) } returns RecursiveCredentialSchema202012DataProvider()
        val vc = Signatory.getService().issue("Europass", ProofConfig(
            issuerDid = did,
            subjectDid = did,
            proofType = ProofType.JWT,
            credentialId = "urn:uuid:${UUID.randomUUID()}"
        ))
        Auditor.getService().verify(vc, listOf(TrustedSchemaRegistryPolicy())).valid shouldBe false
        unmockkAll()
    }

    "Validates 2020-12 schema without recursive refs" {
        mockkObject(DataProviderRegistry)
        every { DataProviderRegistry.getProvider(VerifiableDiploma::class) } returns NotRecursiveCredentialSchema202012DataProvider()
        val vc = Signatory.getService().issue("VerifiableDiploma", ProofConfig(
            issuerDid = did,
            subjectDid = did,
            proofType = ProofType.JWT,
            credentialId = "urn:uuid:${UUID.randomUUID()}"
        ))
        Auditor.getService().verify(vc, listOf(TrustedSchemaRegistryPolicy())).valid shouldBe true
        unmockkAll()
    }

})

class RecursiveCredentialSchema201909DataProvider() : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        DefaultDataProvider.populate(template, proofConfig)
        return populateValidEuropass(
            template as Europass,
            Path.of("src", "test", "resources", "schemas", "2019-09-recursive-ref.json")
        )
    }
}

class RecursiveCredentialSchema202012DataProvider() : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        DefaultDataProvider.populate(template, proofConfig)
        return populateValidEuropass(
            template as Europass,
            Path.of("src", "test", "resources", "schemas", "2020-12-recursive-ref.json")
        )
    }
}

fun populateValidEuropass(europass: Europass, schema: Path) = europass.also {
    it.credentialSubject?.achieved?.get(0)?.also { achieved ->
        achieved.entitlesTo = null
        achieved.hasPart?.learningAchievements = emptyList()
    }
    it.credentialStatus = null
    it.credentialSchema!!.also { credentialSchema ->
        credentialSchema.type = "FullJsonSchemaValidator2021"
        credentialSchema.id = schema.toUri().toString()
    }
}

class NotRecursiveCredentialSchema202012DataProvider() : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, proofConfig: ProofConfig): VerifiableCredential {
        DefaultDataProvider.populate(template, proofConfig)
        return template.also {
            it.proof = Proof(
                type = "Dummy test value",
                created = "2022-01-28T00:00:00Z",
                proofPurpose = "Dummy test value",
                jws = "Dummy test value",
                verificationMethod = "Dummy test value"
            )
            it.credentialSchema!!.id =
                Path.of("src", "test", "resources", "schemas", "2020-12-no-recursive-ref.json")
                    .toUri()
                    .toString()
        }
    }
}