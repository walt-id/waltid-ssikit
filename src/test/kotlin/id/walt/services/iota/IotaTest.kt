package id.walt.services.iota

import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import kotlin.reflect.KClass

class IOTAEnabled : EnabledCondition {
    override fun enabled(kclass: KClass<out Spec>): Boolean {
        return false
    }
}

@EnabledIf(IOTAEnabled::class)
class IotaTest : AnnotationSpec() {

    @BeforeAll
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    @Test
    fun testCreateDid() {
        val key = KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519)
        val did = DidService.create(DidMethod.iota, key.id)
        val doc = DidService.load(did)
        println(doc.encodePretty())

        doc.id shouldStartWith "did:iota"
        doc.method shouldBe DidMethod.iota
        doc.verificationMethod shouldNotBe null
        doc.verificationMethod?.shouldHaveSize(1)
        doc.verificationMethod!!.first().publicKeyMultibase shouldNotBe null
        doc.capabilityInvocation shouldNotBe null
        doc.capabilityInvocation?.shouldHaveSize(1)
        doc.capabilityInvocation!!.first().id shouldBe doc.verificationMethod!!.first().id
        KeyService.getService().hasKey(doc.verificationMethod!!.first().id) shouldBe true

        val docResolved = DidService.resolve(doc.id)
        docResolved shouldNotBe null
        docResolved.encode() shouldEqualJson doc.encode()
    }
}
