package id.walt.context

import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.context.WaltContext
import id.walt.services.did.DidService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.File

class WaltContextTest: AnnotationSpec() {

  @BeforeAll
  fun setup() {
    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    File(TEST_CONTEXT_DATA_ROOT).deleteRecursively()
  }

  @AfterAll
  fun cleanup() {
    File(TEST_CONTEXT_DATA_ROOT).deleteRecursively()
  }

  @Test
  fun testContext() {
    val context = TestContext("userA")
    ServiceRegistry.registerService<WaltContext>(context)
    val did1 = DidService.create(DidMethod.key)

    context.currentContext = "userB"
    val did2 = DidService.create(DidMethod.key)

    val didList2 = DidService.listDids()
    didList2 shouldHaveSize 1
    didList2.first() shouldBe did2

    context.currentContext = "userA"

    val didList1 = DidService.listDids()
    didList1 shouldHaveSize 1
    didList1.first() shouldBe did1
  }
}
