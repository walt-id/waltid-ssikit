package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceConfiguration
import io.ipfs.multibase.binary.Base32
import io.ktor.util.*
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.nio.file.Path

data class FilesystemStoreConfig(
    val dataRoot: String
) : ServiceConfiguration {
    val dataDirectory: Path = Path.of(dataRoot)
}

class FileSystemHKVStore(configPath: String) : HKVStoreService() {

    override lateinit var configuration: FilesystemStoreConfig

    init {
        if (configPath.isNotEmpty()) configuration = fromConfiguration(configPath)
    }

    constructor(config: FilesystemStoreConfig) : this("") {
        configuration = config
    }

    private fun getFinalPath(keyPath: Path): File = dataDirCombinePath(keyPath).apply {
        if (name.length > MAX_KEY_SIZE) {
            val hashedFileNameBytes = DigestUtils.sha3_512(nameWithoutExtension)
            val hashedFileName = Base32().encodeToString(hashedFileNameBytes).replace("=", "").replace("+", "")

            val ext = extension

            val newName = hashedFileName + (ext.ifBlank { "" })

            val newPath = keyPath.parent.resolve(newName)

            println("NEW PATH IS $newPath")

            return dataDirCombinePath(newPath)
        }
    }

    override fun put(key: HKVKey, value: ByteArray) {
        getFinalPath(key.toPath()).apply {
            parentFile.mkdirs()
            writeBytes(value)
        }
    }

    override fun getAsByteArray(key: HKVKey): ByteArray? = getFinalPath(key.toPath()).run {
        return when (exists()) {
            true -> readBytes()
            else -> null
        }
    }

    override fun listChildKeys(parent: HKVKey, recursive: Boolean): Set<HKVKey> =
        getFinalPath(parent.toPath()).listFiles().let { pathFileList ->
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
        getFinalPath(key.toPath()).run { if (recursive) deleteRecursively() else delete() }

    private fun dataDirRelativePath(file: File) = configuration.dataDirectory.relativize(file.toPath())
    private fun dataDirCombinePath(key: Path) = configuration.dataDirectory.combineSafe(key)

    companion object {
        private const val MAX_KEY_SIZE = 120
    }
}
