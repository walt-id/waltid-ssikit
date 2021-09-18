package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceConfiguration
import io.ktor.util.*
import java.io.File
import java.nio.file.Path

data class FilesystemStoreConfig(
    val dataRoot: String
) : ServiceConfiguration {
    val dataDirectory: Path = Path.of(dataRoot)
}

class FileSystemHKVStore(configurationPath: String) : HKVStoreService() {

    override val configuration: FilesystemStoreConfig = fromConfiguration(configurationPath)

    override fun put(key: Path, value: ByteArray) {
        dataDirCombinePath(key).apply {
            parentFile.mkdirs()
            writeBytes(value)
        }
    }

    override fun getAsByteArray(key: Path): ByteArray = dataDirCombinePath(key).readBytes()

    override fun listChildKeys(parent: Path, recursive: Boolean): Set<Path> =
        dataDirCombinePath(parent).listFiles().let { pathFileList ->
            when (recursive) {
                false -> pathFileList?.filter { it.isFile }?.map { dataDirRelativePath(it) }?.toSet()
                true -> pathFileList?.flatMap {
                    dataDirRelativePath(it).let { currentPath ->
                        when {
                            it.isFile -> setOf(currentPath)
                            else -> listChildKeys(currentPath, true)
                        }
                    }
                }?.toSet()
            } ?: emptySet()
        }

    override fun delete(key: Path, recursive: Boolean): Boolean =
        dataDirCombinePath(key).run { if (recursive) deleteRecursively() else delete() }

    private fun dataDirRelativePath(file: File) = configuration.dataDirectory.relativize(file.toPath())
    private fun dataDirCombinePath(key: Path) = configuration.dataDirectory.combineSafe(key)
}
