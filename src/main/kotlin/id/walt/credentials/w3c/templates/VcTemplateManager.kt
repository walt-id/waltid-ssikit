package id.walt.credentials.w3c.templates

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CIssuer
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.services.context.ContextManager
import id.walt.services.hkvstore.HKVKey
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.time.Duration.Companion.hours

object VcTemplateManager {
    private val log = KotlinLogging.logger {}
    const val SAVED_VC_TEMPLATES_KEY = "vc-templates"

    fun register(name: String, template: VerifiableCredential): VcTemplate {
        template.proof = null
        template.issuer = template.issuer?.let { W3CIssuer("", it._isObject, it.properties) }
        template.credentialSubject?.id = null
        template.id = null
        ContextManager.hkvStore.put(HKVKey(SAVED_VC_TEMPLATES_KEY, name), template.toJson())
        return VcTemplate(name, template, true)
    }

    val templateCache = Cache.Builder()
        .maximumCacheSize(1000)
        .expireAfterWrite(1.hours)
        .build<String, VcTemplate>()

    private fun String?.toVcTemplate(name: String, loadTemplate: Boolean, isMutable: Boolean) =
        this?.let { VcTemplate(name, if (loadTemplate) it.toVerifiableCredential() else null, isMutable) }

    fun loadTemplateFromHkvStore(name: String, loadTemplate: Boolean) =
        ContextManager.hkvStore.getAsString(HKVKey(SAVED_VC_TEMPLATES_KEY, name))
            .toVcTemplate(name, loadTemplate, true)

    fun loadTemplateFromResources(name: String, loadTemplate: Boolean) =
        object {}.javaClass.getResource("/vc-templates/$name.json")?.readText()
            .toVcTemplate(name, loadTemplate, false)

    fun loadTemplateFromFile(name: String, loadTemplate: Boolean, runtimeTemplateFolder: String) =
        File("$runtimeTemplateFolder/$name.json").let {
            if (it.exists()) it.readText() else null
        }.toVcTemplate(name, loadTemplate, false)

    fun getTemplate(
        name: String,
        loadTemplate: Boolean = true,
        runtimeTemplateFolder: String = "/vc-templates-runtime"
    ): VcTemplate =
        runBlocking {
            templateCache.get(name) {
                loadTemplateFromHkvStore(name, true)//.also { println("At $name HKV: $it") }
                    ?: loadTemplateFromResources(name, true)//.also { println("At $name resources: $it") }
                    ?: loadTemplateFromFile(name, true, runtimeTemplateFolder)//.also { println("At $name file: $it") }
                    ?: throw IllegalArgumentException("No template found with name: $name")
            }
        }

    private val resourceWalk = lazy {

        val resource = object {}.javaClass.getResource("/vc-templates")!!
        when {
            File(resource.file).isDirectory ->
                File(resource.file).walk().filter { it.isFile }.map { it.nameWithoutExtension }.toList()

            else -> {
                FileSystems.newFileSystem(resource.toURI(), emptyMap<String, String>()).use { fs ->
                    Files.walk(fs.getPath("/vc-templates"))
                        .filter { it.isRegularFile() }
                        .map { it.nameWithoutExtension }.toList()
                }
            }
        }
    }

    private fun listResources(): List<String> = resourceWalk.value


    private fun listRuntimeTemplates(folderPath: String): List<String> {
        val templatesFolder = File(folderPath)
        return if (templatesFolder.isDirectory) {
            templatesFolder.walk().filter { it.isFile }.map { it.nameWithoutExtension }.toList()
        } else {
            log.warn { "Requested runtime templates folder $folderPath is not a folder." }
            listOf()
        }
    }

    fun listTemplates(runtimeTemplateFolder: String = "/vc-templates-runtime"): List<VcTemplate> {
        return listResources()
            .plus(ContextManager.hkvStore.listChildKeys(HKVKey(SAVED_VC_TEMPLATES_KEY), false).map { it.name })
            .plus(listRuntimeTemplates(runtimeTemplateFolder))
            .toSet().map { getTemplate(it, false, runtimeTemplateFolder) }.toList()
    }

    fun unregisterTemplate(name: String) {
        ContextManager.hkvStore.delete(HKVKey(SAVED_VC_TEMPLATES_KEY, name))
    }
}
