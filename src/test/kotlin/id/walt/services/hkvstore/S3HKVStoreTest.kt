package id.walt.services.hkvstore

import id.walt.services.essif.EBSI_BEARER_TOKEN_FILE
import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.reflect.KClass

class S3TestEnabled: EnabledCondition {
  override fun enabled(kclass: KClass<out Spec>): Boolean {
    return File("testS3Store.conf").exists()
  }
}

@EnabledIf(S3TestEnabled::class)
class S3HKVStoreTest: AnnotationSpec() {

  @BeforeClass
  fun init() {
  }

  @Test
  fun testS3HkvStore() {
    val s3Store = S3HKVStore("testS3Store.conf")

    val k1 = HKVKey("root", "child", "leaf1")
    val k2 = HKVKey("root", "child", "leaf2")
    s3Store.put(k1, "mydata1")
    s3Store.put(k2, "mydata2")

    val recList = s3Store.listChildKeys(HKVKey("root"), recursive = true)
    recList.size shouldBe 2
    recList shouldContainExactly setOf(k1, k2)

    s3Store.getAsString(k1) shouldBe "mydata1"
    s3Store.getAsString(k2) shouldBe "mydata2"
  }
}

