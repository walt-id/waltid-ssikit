package id.walt.services.hkvstore

import io.ktor.util.*
import java.nio.file.Path

class FileSystemHKVStore(val dataFolder: Path) : HierarchicalKeyValueStoreService() {
    override fun put(key: Path, value: ByteArray) {
        dataFolder.combineSafe(key.parent).mkdirs()
        dataFolder.combineSafe(key).writeBytes(value)
    }

    override fun getAsByteArray(key: Path): ByteArray {
        return dataFolder.combineSafe(key).readBytes()
    }

    override fun listKeys(parent: Path, recursive: Boolean): Set<Path> {
        return when(recursive) {
            false -> dataFolder.combineSafe(parent).listFiles()?.filter { it.isFile }?.map { dataFolder.relativize(it.toPath()) }?.toSet() ?: setOf()
            else -> dataFolder.combineSafe(parent).listFiles()?.flatMap {
                val currPath = dataFolder.relativize(it.toPath())
                when(it.isFile){
                    true -> setOf(currPath)
                    false -> listKeys(currPath, true)
                } }?.toSet() ?: setOf()
        }

    }

    override fun delete(key: Path, recursive: Boolean) {
        when(recursive) {
            true -> dataFolder.combineSafe(key).deleteRecursively()
            false -> dataFolder.combineSafe(key).delete()
        }
    }
}