package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceConfiguration
import id.walt.signatory.ProofConfig
import id.walt.signatory.SignatoryConfig
import io.ktor.util.*
import java.nio.file.Path

data class FilesystemStoreConfig(
    val dataRoot: String
) : ServiceConfiguration {
    val dataFolder = Path.of(dataRoot)
}

class FileSystemHKVStore(configurationPath: String) : HierarchicalKeyValueStoreService() {

    override val configuration: FilesystemStoreConfig = fromConfiguration(configurationPath)

    override fun put(key: Path, value: ByteArray) {
        configuration.dataFolder.combineSafe(key.parent).mkdirs()
        configuration.dataFolder.combineSafe(key).writeBytes(value)
    }

    override fun getAsByteArray(key: Path): ByteArray {
        return configuration.dataFolder.combineSafe(key).readBytes()
    }

    override fun listKeys(parent: Path, recursive: Boolean): Set<Path> {
        return when(recursive) {
            false -> configuration.dataFolder.combineSafe(parent).listFiles()?.filter { it.isFile }?.map { configuration.dataFolder.relativize(it.toPath()) }?.toSet() ?: setOf()
            else -> configuration.dataFolder.combineSafe(parent).listFiles()?.flatMap {
                val currPath = configuration.dataFolder.relativize(it.toPath())
                when(it.isFile){
                    true -> setOf(currPath)
                    false -> listKeys(currPath, true)
                } }?.toSet() ?: setOf()
        }

    }

    override fun delete(key: Path, recursive: Boolean) {
        when(recursive) {
            true -> configuration.dataFolder.combineSafe(key).deleteRecursively()
            false -> configuration.dataFolder.combineSafe(key).delete()
        }
    }
}