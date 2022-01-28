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
import java.nio.file.Path
import java.util.*

class SchemaValidatorTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val did = DidService.create(DidMethod.key)

    "Validates 2019-09 schema with recursive refs" {
        DataProviderRegistry.register(Europass::class, RecursiveCredentialSchema201909DataProvider())
        val vc = Signatory.getService().issue("Europass", ProofConfig(
            issuerDid = did,
            subjectDid = did,
            proofType = ProofType.JWT,
            credentialId = "urn:uuid:${UUID.randomUUID()}"
        ))
        Auditor.getService().verify(vc, listOf(TrustedSchemaRegistryPolicy())).valid shouldBe true
    }

    "Does not validate 2020-12 schema with recursive refs" {
        DataProviderRegistry.register(Europass::class, RecursiveCredentialSchema202012DataProvider())
        val vc = Signatory.getService().issue("Europass", ProofConfig(
            issuerDid = did,
            subjectDid = did,
            proofType = ProofType.JWT,
            credentialId = "urn:uuid:${UUID.randomUUID()}"
        ))
        Auditor.getService().verify(vc, listOf(TrustedSchemaRegistryPolicy())).valid shouldBe false
    }

    "Validates 2020-12 schema without recursive refs" {
        DataProviderRegistry.register(VerifiableDiploma::class, NotRecursiveCredentialSchema202012DataProvider())
        val vc = Signatory.getService().issue("VerifiableDiploma", ProofConfig(
            issuerDid = did,
            subjectDid = did,
            proofType = ProofType.JWT,
            credentialId = "urn:uuid:${UUID.randomUUID()}"
        ))
        Auditor.getService().verify(vc, listOf(TrustedSchemaRegistryPolicy())).valid shouldBe true
    }

    "test" {
        val vp = "eyJraWQiOiJkaWQ6ZWJzaTp6eVJ2TFZqQ2hlajFmWlFZVEs1TGtiNyIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2SyJ9.eyJzdWIiOiJkaWQ6ZWJzaTp6eVJ2TFZqQ2hlajFmWlFZVEs1TGtiNyIsImF1ZCI6ImRpZDplYnNpOnprZGExdDZqTEJ0UzVHeFdDVmRyQXJIIiwibmJmIjoxNjQzMzg0MjQzLCJpc3MiOiJkaWQ6ZWJzaTp6eVJ2TFZqQ2hlajFmWlFZVEs1TGtiNyIsInZwIjp7ImhvbGRlciI6ImRpZDplYnNpOnp5UnZMVmpDaGVqMWZaUVlUSzVMa2I3IiwidHlwZSI6WyJWZXJpZmlhYmxlUHJlc2VudGF0aW9uIl0sIkBjb250ZXh0IjpbImh0dHBzOlwvXC93d3cudzMub3JnXC8yMDE4XC9jcmVkZW50aWFsc1wvdjEiXSwidmVyaWZpYWJsZUNyZWRlbnRpYWwiOlsiZXlKcmFXUWlPaUprYVdRNlpXSnphVHA2YzJGMFUyODNjbnA2Y2pKTFRrMURWbGhxZEdsQlpDSXNJblI1Y0NJNklrcFhWQ0lzSW1Gc1p5STZJa1ZUTWpVMlN5SjkuZXlKemRXSWlPaUprYVdRNlpXSnphVHA2ZVZKMlRGWnFRMmhsYWpGbVdsRlpWRXMxVEd0aU55SXNJbTVpWmlJNk1UWTBNek15T0RBd01Dd2lhWE56SWpvaVpHbGtPbVZpYzJrNmVuTmhkRk52TjNKNmVuSXlTMDVOUTFaWWFuUnBRV1FpTENKcFlYUWlPakUyTkRNek1qZ3dNREFzSW5aaklqcDdJbU55WldSbGJuUnBZV3hUWTJobGJXRWlPbnNpYVdRaU9pSm9kSFJ3Y3pwY0wxd3ZZWEJwTG5CeVpYQnliMlF1WldKemFTNWxkVnd2ZEhKMWMzUmxaQzF6WTJobGJXRnpMWEpsWjJsemRISjVYQzkyTVZ3dmMyTm9aVzFoYzF3dk1IZzFPR1l3T0RkbVptTmtNRFZsWlRjeE1UVTNNakprTWpKbVlqa3hPR1JpTkRSbVpESTFZamxsT1RnMU1UZzJNRE5pTkdSa01HRXpOVE0wWmpReU1EWXlJaXdpZEhsd1pTSTZJa1oxYkd4S2MyOXVVMk5vWlcxaFZtRnNhV1JoZEc5eU1qQXlNU0o5TENKamNtVmtaVzUwYVdGc1UzVmlhbVZqZENJNmV5Sm1hWEp6ZEU1aGJXVWlPaUpLWVc1bElpd2lhV1JsYm5ScFptbGxjaUk2VzNzaWMyTm9aVzFsU1VRaU9pSkZkWEp2Y0dWaGJpQnpkSFZrWlc1MElHbGtaVzUwYVdacFpYSWlMQ0oyWVd4MVpTSTZJblZ5YmpwelkyaGhZenB3WlhKemIyNWhiRlZ1YVhGMVpVTnZaR1U2YVc1ME9tVnphVHBtY2pvd09UQTBNREE0TURnMFNDSjlYU3dpY0d4aFkyVlBaa0pwY25Sb0lqb2lVMkZwYm5RdFEyeHZkV1FzSUVaU1FVNURSU0lzSW1aaGJXbHNlVTVoYldVaU9pSkVUMFVpTENKa1lYUmxUMlpDYVhKMGFDSTZJakU1TnpjdE1UQXRNalFpZlN3aWRIbHdaU0k2V3lKV1pYSnBabWxoWW14bFEzSmxaR1Z1ZEdsaGJDSXNJbFpsY21sbWFXRmliR1ZCZEhSbGMzUmhkR2x2YmlJc0lsWmxjbWxtYVdGaWJHVkpaQ0pkTENKQVkyOXVkR1Y0ZENJNld5Sm9kSFJ3Y3pwY0wxd3ZkM2QzTG5jekxtOXlaMXd2TWpBeE9Gd3ZZM0psWkdWdWRHbGhiSE5jTDNZeElsMTlMQ0pxZEdraU9pSjFjbTQ2ZFhWcFpEbzRZamRoTlRVeE9DMDNNR1JqTFRSaVpETXRPV015T0MwMk1URXdOVGRpT0RNeVpqTWlmUS54X1pkRkFQLTlsWkp4R1VTU0xFYWNTc24yVU5IckNYWjVEUTVYTVlZMkV1TmRTaFdzdVVnNDJuTnBMZmw3akNhOVd0ZmFyYndnWkdfczdvZTBSbzAwUSIsImV5SnJhV1FpT2lKa2FXUTZaV0p6YVRwNmMyRjBVMjgzY25wNmNqSkxUazFEVmxocWRHbEJaQ0lzSW5SNWNDSTZJa3BYVkNJc0ltRnNaeUk2SWtWVE1qVTJTeUo5LmV5SnpkV0lpT2lKa2FXUTZaV0p6YVRwNmVWSjJURlpxUTJobGFqRm1XbEZaVkVzMVRHdGlOeUlzSW01aVppSTZNVFkwTXpNeU9EQXdNQ3dpYVhOeklqb2laR2xrT21WaWMyazZlbk5oZEZOdk4zSjZlbkl5UzA1TlExWllhblJwUVdRaUxDSnBZWFFpT2pFMk5ETXpNamd3TURBc0luWmpJanA3SW1OeVpXUmxiblJwWVd4VFkyaGxiV0VpT25zaWFXUWlPaUpvZEhSd2N6cGNMMXd2WVhCcExuQnlaWEJ5YjJRdVpXSnphUzVsZFZ3dmRISjFjM1JsWkMxelkyaGxiV0Z6TFhKbFoybHpkSEo1WEM5Mk1Wd3ZjMk5vWlcxaGMxd3ZNSGc1TWpoak1HVmlZalZqWm1NM01tTTNNR1V4TWpjek9ERXlaV0l4Tm1ZMk4yWmhORGt5WXpVeVlXTTVZell6TXpCbU5XVTVORE0zTkdReVpqUTROVEF5SWl3aWRIbHdaU0k2SWtaMWJHeEtjMjl1VTJOb1pXMWhWbUZzYVdSaGRHOXlNakF5TVNKOUxDSmpjbVZrWlc1MGFXRnNVM1ZpYW1WamRDSTZleUpoWTJocFpYWmxaQ0k2VzNzaWQyRnpRWGRoY21SbFpFSjVJanA3SW1GM1lYSmthVzVuVEc5allYUnBiMjRpT2xzaWFIUjBjSE02WEM5Y0wyUnBjR3h2YldVdFkyVnlkR2xtYVdOaGRDNTFibWwyTFd4cGJHeGxMbVp5WEM5dmJuUnZiRzluZVNOQmQyRnlaR2x1WjB4dlkyRjBhVzl1SWwwc0ltRjNZWEprYVc1blJHRjBaU0k2SWpJd01qRXRNVEF0TVRsVU1EQTZNREE2TURCYUlpd2lZWGRoY21ScGJtZENiMlI1SWpwYkltUnBaRHBsWW5OcE9ucHpZWFJUYnpkeWVucHlNa3RPVFVOV1dHcDBhVUZrSWwwc0ltbGtJam9pYUhSMGNITTZYQzljTDJScGNHeHZiV1V0WTJWeWRHbG1hV05oZEM1MWJtbDJMV3hwYkd4bExtWnlYQzl2Ym5SdmJHOW5lU05CZDJGeVpHbHVaMUJ5YjJObGMzTWlmU3dpYVdSbGJuUnBabWxsY2lJNlczc2ljMk5vWlcxbFNVUWlPaUpvZEhSd2N6cGNMMXd2Wkdsd2JHOXRaUzFqWlhKMGFXWnBZMkYwTG5WdWFYWXRiR2xzYkdVdVpuSmNMMjl1ZEc5c2IyZDVJMVZNYVd4c1pVTnlaV1JsYm5ScFlXeHpJaXdpZG1Gc2RXVWlPaUpvZEhSd2N6cGNMMXd2Wkdsd2JHOXRaUzFqWlhKMGFXWnBZMkYwTG5WdWFYWXRiR2xzYkdVdVpuSmNMMmx1WkdWNExtaDBiV3dfYTJWNVBUTTFNakpGUVRJMk9UUTBOVFpDUWtORFJrUkVRamRFUmpNeU16SkZOa1pGT1VVNU1UZERRMFUwTXpoQlJUQTRNall6TWpZek9UQTFRVGRETXpreE56QmphM2hIWW10a1YxbFZTakJYVldSSVdsVk9kRmxzUmxWUmJtUjNaREk1YUZNd1RrdFJlWFEyVG1wQ00xSkVSVEpPYWtwQ1UzcEdjVlpzWkhWYU1IUk1JbjFkTENKemNHVmphV1pwWldSQ2VTSTZXM3NpYVdRaU9pSm9kSFJ3Y3pwY0wxd3ZaR2x3Ykc5dFpTMWpaWEowYVdacFkyRjBMblZ1YVhZdGJHbHNiR1V1Wm5KY0wyOXVkRzlzYjJkNUkwRnpjMlZ6YzIxbGJuUlRjR1ZqYVdacFkyRjBhVzl1SWl3aVpVTlVVME55WldScGRGQnZhVzUwY3lJNk5qQjlYU3dpWVdSa2FYUnBiMjVoYkU1dmRHVWlPbHNpVFVWVVNVVlNVeUJFVlNCRFQwMU5SVkpEUlNCSlRsUkZVazVCVkVsUFRrRk1JaXdpUTA5TlRVVlNRMFVnU1U1VVJWSk9RVlJKVDA1QlRDQkZUaUJRVFVVaUxDSXlNREU1TFRJd01qQWlYU3dpZDJGelJHVnlhWFpsWkVaeWIyMGlPbHQ3SW1keVlXUmxJam9pUVhOelpYb2dRbWxsYmlJc0ltbGtJam9pYUhSMGNITTZYQzljTDJScGNHeHZiV1V0WTJWeWRHbG1hV05oZEM1MWJtbDJMV3hwYkd4bExtWnlYQzl2Ym5SdmJHOW5lU05CYzNObGMzTnRaVzUwSWl3aWRHbDBiR1VpT2lKUGRtVnlZV3hzSUVScGNHeHZiV0VnUVhOelpYTnpiV1Z1ZENKOVhTd2lhV1FpT2lKb2RIUndjenBjTDF3dlpHbHdiRzl0WlMxalpYSjBhV1pwWTJGMExuVnVhWFl0Ykdsc2JHVXVabkpjTDI5dWRHOXNiMmQ1STB4bFlYSnVhVzVuUVdOb2FXVjJaVzFsYm5RaUxDSjBhWFJzWlNJNklreEpRMFZPUTBVZ1VGSlBSa1ZUVTBsUFRrNUZURXhGSUVSU1QwbFVMQ0JGUTA5T1QwMUpSU3dnUjBWVFZFbFBUaUo5WFgwc0luUjVjR1VpT2xzaVZtVnlhV1pwWVdKc1pVTnlaV1JsYm5ScFlXd2lMQ0pXWlhKcFptbGhZbXhsUVhSMFpYTjBZWFJwYjI0aUxDSkZkWEp2Y0dGemN5SmRMQ0pBWTI5dWRHVjRkQ0k2V3lKb2RIUndjenBjTDF3dmQzZDNMbmN6TG05eVoxd3ZNakF4T0Z3dlkzSmxaR1Z1ZEdsaGJITmNMM1l4SWwxOUxDSnFkR2tpT2lKMWNtNDZkWFZwWkRvNU16VTBPV1UxTnkwMk5XTTNMVFExWlRJdE9HSTNOaTFqT1ROa1l6VXhNMk0zTldJaWZRLkwtLXdua0U5Q2w0QW1uSjM1Qy11cU1odHkwX2JocUczRXhhSXZuY1dzZ0JsLXpNUmhfVnd4SURVVUdpZVE3bmlBOG5mdEozNXBhdlo3d0g0N2dzU0ZnIl19LCJleHAiOjMzMTY4NDc4NTMwLCJpYXQiOjE2NDMzODQyNDMsImp0aSI6InVybjp1dWlkOjM1NDVlNTE2LWY1YzktNGU2YS1iY2U1LTJhNzYxZTNiMzJlMyJ9.wD9N09Illuj5FgCZ_Rsm36Fz7R0Nzy-jE5ygxOIO7ORukA1E_WwHPgLl1UEE3Vz2DrpHafpFTQsryTjiU35rJA"
        Auditor.getService().verify(vp, listOf(TrustedSchemaRegistryPolicy())).valid shouldBe true
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