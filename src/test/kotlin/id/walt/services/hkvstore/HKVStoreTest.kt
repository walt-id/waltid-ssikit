package id.walt.services.hkvstore

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class HKVStoreTest : StringSpec({

    fun hkvTest(hkvStore: HierarchicalKeyValueStoreService) {
        val testData = "test data"

        println("Adding items...")
        hkvStore.put(Path.of("parent", "child", "leaf"), testData)
        hkvStore.put(Path.of("parent", "child2", "leaf2"), testData)

        println("Retrieving items...")
        hkvStore.getChildKeys(Path.of("parent"), true) shouldHaveSize 2
        hkvStore.getChildKeys(Path.of("parent", "child")) shouldHaveSize 1

        println("Retrieving data...")
        hkvStore.getAsString(Path.of("parent", "child", "leaf")) shouldBe testData

        println("Deleting items...")
        hkvStore.delete(Path.of("parent"), true)
        hkvStore.getChildKeys(Path.of("parent"), true) shouldHaveSize 0
    }

    "InMemoryStore test" {
        hkvTest(InMemoryHKVStore())
    }
    "FileSystemStore test" {
        hkvTest(FileSystemHKVStore("./fsStore.conf"))
    }
})
