package id.walt.credentials.w3c.templates

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.services.context.ContextManager
import id.walt.services.hkvstore.HKVKey
import mu.KotlinLogging
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension

object VcTemplateManager {
    private val log = KotlinLogging.logger {}
    const val SAVED_VC_TEMPLATES_KEY = "vc-templates"

    fun register(name: String, template: VerifiableCredential): VcTemplate {
        template.proof = null
        template.issuer = null
        template.credentialSubject?.id = null
        template.id = null
        ContextManager.hkvStore.put(HKVKey(SAVED_VC_TEMPLATES_KEY, name), template.toJson())
        return VcTemplate(name, template, true)
    }

    fun getTemplate(
        name: String,
        loadTemplate: Boolean = true,
        runtimeTemplateFolder: String = "/vc-templates-runtime"
    ): VcTemplate {
        return ContextManager.hkvStore.getAsString(HKVKey(SAVED_VC_TEMPLATES_KEY, name))
            ?.let { VcTemplate(name, if (loadTemplate) it.toVerifiableCredential() else null, true) }
            ?: object {}.javaClass.getResource("/vc-templates/$name.json")?.readText()
                ?.let { VcTemplate(name, if (loadTemplate) it.toVerifiableCredential() else null, false) }
            ?: File("$runtimeTemplateFolder/$name.json").let {
                if (it.exists()) it.readText() else null
            }?.let { VcTemplate(name, if (loadTemplate) it.toVerifiableCredential() else null, false) }
            ?: throw IllegalArgumentException("No template found, with name $name")
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
