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

    override fun put(key: HKVKey, value: ByteArray) {
        dataDirCombinePath(key.toPath()).apply {
            parentFile.mkdirs()
            writeBytes(value)
        }
    }

    override fun getAsByteArray(key: HKVKey): ByteArray? = dataDirCombinePath(key.toPath()).run {
        return when(exists()) {
            true -> readBytes()
            else -> null
        }
    }

    override fun listChildKeys(parent: HKVKey, recursive: Boolean): Set<HKVKey> =
        dataDirCombinePath(parent.toPath()).listFiles().let { pathFileList ->
            when (recursive) {
                false -> pathFileList?.filter { it.isFile }?.map { HKVKey.fromPath(dataDirRelativePath(it)) }?.toSet()
                true -> pathFileList?.flatMap {
                    HKVKey.fromPath(dataDirRelativePath(it)).let { currentPath ->
                        when {
                            it.isFile -> setOf(currentPath)
                            else -> listChildKeys(currentPath, true)
                        }
                    }
                }?.toSet()
            } ?: emptySet()
        }

    override fun delete(key: HKVKey, recursive: Boolean): Boolean =
        dataDirCombinePath(key.toPath()).run { if (recursive) deleteRecursively() else delete() }

    private fun dataDirRelativePath(file: File) = configuration.dataDirectory.relativize(file.toPath())
    private fun dataDirCombinePath(key: Path) = configuration.dataDirectory.combineSafe(key)
}
