package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceConfiguration
import io.ipfs.multibase.binary.Base32
import io.ktor.util.*
import mu.KotlinLogging
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

data class FilesystemStoreConfig(
    val dataRoot: String,
    val maxKeySize: Int = 111
) : ServiceConfiguration {
    val dataDirectory: Path = Path.of(dataRoot)
}

class FileSystemHKVStore(configPath: String) : HKVStoreService() {

    override lateinit var configuration: FilesystemStoreConfig

    constructor(config: FilesystemStoreConfig) : this("") {
        configuration = config
    }

    private val logger = KotlinLogging.logger { }

    init {
        if (configPath.isNotEmpty()) configuration = fromConfiguration(configPath)
    }

    private val mappingFilePath = lazy {
        configuration.dataDirectory.resolve("hash-mappings.properties").apply {
            if (notExists()) Properties().store(this.bufferedWriter(), hashMappingDesc)
        }
    }
    private val mappingProperties = lazy {
        if (mappingFilePath.value.notExists()) Properties().store(mappingFilePath.value.bufferedWriter(), hashMappingDesc)

        Properties().apply { load(mappingFilePath.value.bufferedReader()) }
    }

    private fun storeMappings() = mappingProperties.value.store(mappingFilePath.value.bufferedWriter(), hashMappingDesc)

    private fun storeHashMapping(keyName: String, hashMapping: String) {
        logger.debug { "Mapping \"$keyName\" to \"$hashMapping\"" }
        mappingProperties.value[hashMapping] = keyName
        storeMappings()
    }

    private fun retrieveHashMapping(hashMapping: String): String = mappingProperties.value[hashMapping] as? String
        ?: throw IllegalArgumentException("No HKVS mapping found for hash: $hashMapping")

    private fun hashIfNeeded(path: Path): File {
        if (path.name.length > configuration.maxKeySize) {
            val hashedFileNameBytes = DigestUtils.sha3_512(path.name)
            val hashedFileName = Base32().encodeToString(hashedFileNameBytes).replace("=", "").replace("+", "")

            //val ext = extension

            val newName = hashedFileName //+ (ext.ifBlank { "" })

            val newPath = path.parent.resolve(newName)

            storeHashMapping(path.name, newName)

            logger.debug { "File mapping is hashed: Path was \"${path.absolutePathString()}\", new path is $newPath" }
            return dataDirCombinePath(dataDirRelativePath(newPath))
        }
        return path.toFile()
    }

    private fun getFinalPath(keyPath: Path): File = hashIfNeeded(dataDirCombinePath(keyPath).toPath())

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
                false -> pathFileList?.filter { it.isFile }?.map {
                    var mapping = it.toPath()
                    if (mapping.name.length > configuration.maxKeySize) {
                        mapping = mapping.parent.resolve(retrieveHashMapping(mapping.name))
                    }

                    HKVKey.fromPath(dataDirRelativePath(mapping))
                }?.toSet()

                true -> pathFileList?.flatMap {
                    var mapping = it.toPath()
                    if (mapping.name.length > configuration.maxKeySize) {
                        mapping = mapping.parent.resolve(retrieveHashMapping(mapping.name))
                    }

                    HKVKey.fromPath(dataDirRelativePath(mapping)).let { currentPath ->
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
    private fun dataDirRelativePath(path: Path) = configuration.dataDirectory.relativize(path)
    private fun dataDirCombinePath(key: Path) = configuration.dataDirectory.combineSafe(key)

    companion object {
        private const val hashMappingDesc = "FileSystemHKVStore hash mappings properties"
    }
}
