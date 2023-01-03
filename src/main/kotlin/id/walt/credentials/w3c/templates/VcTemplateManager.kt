package id.walt.credentials.w3c.templates

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.AzureKeyVaultConfig
import id.walt.servicematrix.ServiceConfiguration
import id.walt.services.context.ContextManager
import id.walt.services.hkvstore.HKVKey
import io.javalin.core.util.JavalinLogger.enabled
import jdk.jfr.Enabled
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
        template.credentialSubject?.id =  null
        template.id = null
        ContextManager.hkvStore.put(HKVKey(SAVED_VC_TEMPLATES_KEY, name), template.toJson())
        return VcTemplate(name, template, true)
    }

    fun getTemplate(
        name: String,
        loadTemplate: Boolean = true,
        runtimeTemplateFolder: String = "/runtime-templates"
    ): VcTemplate {
        return ContextManager.hkvStore.getAsString(HKVKey(SAVED_VC_TEMPLATES_KEY, name))
            ?.let { VcTemplate(name, if (loadTemplate) it.toVerifiableCredential() else null, true) }
            ?: object {}.javaClass.getResource("/vc-templates/$name.json")?.readText()
                ?.let { VcTemplate(name, if (loadTemplate) it.toVerifiableCredential() else null, false) }
            ?: File("$runtimeTemplateFolder/$name.json")?.readText()
                ?.let { VcTemplate(name, if (loadTemplate) it.toVerifiableCredential() else null, false) }
            ?: throw Exception("No template found, with name $name")
    }

    private fun listResources(resourcePath: String): List<String> {
        val resource = object {}.javaClass.getResource(resourcePath)
        return if (File(resource.file).isDirectory) {
            File(resource.file).walk().filter { it.isFile }.map { it.nameWithoutExtension }.toList()
        } else {
            FileSystems.newFileSystem(resource.toURI(), emptyMap<String, String>()).use { fs ->
                Files.walk(fs.getPath(resourcePath))
                    .filter { it.isRegularFile() }
                    .map { it.nameWithoutExtension }.toList()
            }
        }
    }

    private fun listRuntimeTemplates(folderPath: String): List<String> {
        val templatesFolder = File(folderPath)
        return if (templatesFolder.isDirectory) {
            templatesFolder.walk().filter { it.isFile }.map { it.nameWithoutExtension }.toList()
        } else {
            log.warn { "Requested runtime templates folder$folderPath is not a folder." }
            listOf()
        }
    }

    fun listTemplates(runtimeTemplateFolder: String = "/runtime-templates"): List<VcTemplate> {
        return listResources("/vc-templates")
            .plus(ContextManager.hkvStore.listChildKeys(HKVKey(SAVED_VC_TEMPLATES_KEY), false).map { it.name })
            .plus(listRuntimeTemplates(runtimeTemplateFolder))
            .toSet().map { getTemplate(it, false, runtimeTemplateFolder) }.toList()
    }

    fun unregisterTemplate(name: String) {
        ContextManager.hkvStore.delete(HKVKey(SAVED_VC_TEMPLATES_KEY, name))
    }
}
