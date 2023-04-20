package id.walt.auditor

import id.walt.auditor.policies.EbsiTrustedIssuerRegistryPolicy
import id.walt.auditor.policies.EbsiTrustedIssuerRegistryPolicyArg
import id.walt.auditor.policies.SignaturePolicy
import id.walt.model.TrustedIssuerType
import id.walt.services.ecosystems.essif.TrustedIssuerClient
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import org.junit.jupiter.api.Assertions.assertAll

class PolicyFactoryTest : AnnotationSpec() {

    private val testArgument = EbsiTrustedIssuerRegistryPolicyArg("testArg", TrustedIssuerType.TI)
    private val wrongArg = AnotherArg("Else")
    private val defaultArg = EbsiTrustedIssuerRegistryPolicyArg(
        "${TrustedIssuerClient.domain}/${TrustedIssuerClient.trustedIssuerPath}",
        TrustedIssuerType.TI
    )

    @Test
    fun createTrustedIssuerRegistryPolicyWithoutOptionalArg() {
        val factoryToTest =
            PolicyFactory(EbsiTrustedIssuerRegistryPolicy::class, EbsiTrustedIssuerRegistryPolicyArg::class, "testPolicy", null, false)

        assertAll(
            { factoryToTest.create(testArgument).argument shouldBe testArgument },
            { shouldThrow<IllegalArgumentException> { factoryToTest.create() } },
            { shouldThrow<IllegalArgumentException> { factoryToTest.create(wrongArg) } }
        )
    }

    @Test
    fun createTrustedIssuerRegistryPolicyWithOptionalArg() {
        val factoryToTest =
            PolicyFactory(EbsiTrustedIssuerRegistryPolicy::class, EbsiTrustedIssuerRegistryPolicyArg::class, "testPolicy", null, true)
        assertAll(
            { factoryToTest.create(testArgument).argument shouldBe testArgument },
            { factoryToTest.create().argument shouldBe defaultArg },
            { shouldThrow<IllegalArgumentException> { factoryToTest.create(wrongArg) } }
        )
    }

    @Test
    fun createSimplePolicy() {
        val factoryToTest =
            PolicyFactory<SignaturePolicy, Unit>(SignaturePolicy::class, null, "testPolicy", null)

        assertAll(
            { factoryToTest.create() should beInstanceOf<SignaturePolicy>() },
            { factoryToTest.create(testArgument) shouldBe beInstanceOf<SignaturePolicy>() }
        )
    }
}


data class AnotherArg(val somethingElse: String)
