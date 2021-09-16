package id.walt.services.hkvstore

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class HKVStoreTest: AnnotationSpec() {

    fun hkvTest(hkvStore: HierarchicalKeyValueStoreService) {
        val TEST_DATA = "test data"
        hkvStore.put(Path.of("parent", "child", "leaf"), TEST_DATA)
        hkvStore.put(Path.of("parent", "child2", "leaf2"), TEST_DATA)

        hkvStore.listKeys(Path.of("parent"), true) shouldHaveSize 2
        hkvStore.listKeys(Path.of("parent", "child")) shouldHaveSize  1

        hkvStore.getAsString(Path.of("parent", "child", "leaf")) shouldBe TEST_DATA

        hkvStore.delete(Path.of("parent"), true)
        hkvStore.listKeys(Path.of("parent"), true) shouldHaveSize 0
    }

    @Test
    fun testInMemoryStore() {
        hkvTest(InMemoryHKVStore())
    }

    @Test
    fun testFSStore() {
        hkvTest(FileSystemHKVStore("./fsStore.conf"))
    }
}