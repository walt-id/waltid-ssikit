package id.walt.credentials.w3c.templates

import com.github.benmanes.caffeine.cache.Caffeine
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CIssuer
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.WaltIdService
import id.walt.services.context.ContextManager
import id.walt.services.hkvstore.HKVKey
import mu.KotlinLogging
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

open class VcTemplateService(private val resourcePath: String = "/vc-templates") : WaltIdService() {
    override val implementation: VcTemplateService get() = serviceImplementation()
    private val log = KotlinLogging.logger {}

    companion object : ServiceProvider {
        override fun getService() = ServiceRegistry.getService(VcTemplateService::class)
        override fun defaultImplementation() = VcTemplateService()
        const val SAVED_VC_TEMPLATES_KEY = "vc-templates"
    }

    fun register(name: String, template: VerifiableCredential): VcTemplate {
        template.proof = null
        template.issuer = template.issuer?.let { W3CIssuer("", it._isObject, it.properties) }
        template.credentialSubject?.id = null
        template.id = null
        ContextManager.hkvStore.put(HKVKey(SAVED_VC_TEMPLATES_KEY, name), template.toJson())
        return VcTemplate(name, template, true)
    }

    private val templateCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build<String, VcTemplate>()

    private fun String?.toVcTemplate(name: String, populateTemplate: Boolean, isMutable: Boolean) =
        this?.let {
            VcTemplate(
                name,
                if (populateTemplate && it.isNotBlank()) it.toVerifiableCredential() else null,
                isMutable
            )
        }

    private fun loadTemplateFromHkvStore(name: String, loadTemplate: Boolean) =
        ContextManager.hkvStore.getAsString(HKVKey(SAVED_VC_TEMPLATES_KEY, name))
            .toVcTemplate(name, loadTemplate, true)

    private fun loadTemplateFromResources(name: String, populateTemplate: Boolean) =
        object {}.javaClass.getResource("$resourcePath/$name.json")?.readText()
            .toVcTemplate(name, populateTemplate, false)

    private fun loadTemplateFromFile(name: String, populateTemplate: Boolean, runtimeTemplateFolder: String) =
        File("$runtimeTemplateFolder/$name.json").let {
            if (it.exists()) it.readText() else null
        }.toVcTemplate(name, populateTemplate, false)

    private fun loadTemplate(name: String, populateTemplate: Boolean, runtimeTemplateFolder: String) =
        loadTemplateFromHkvStore(name, populateTemplate)
            ?: loadTemplateFromResources(name, populateTemplate)
            ?: loadTemplateFromFile(name, populateTemplate, runtimeTemplateFolder)
            ?: throw IllegalArgumentException("No template found with name: $name")

    private fun retrieveOrLoadCachedTemplate(
        name: String,
        populateTemplate: Boolean = true,
        runtimeTemplateFolder: String = "/vc-templates-runtime"
    ) = if (populateTemplate) {
        // only add to cache, if template is populated
        templateCache.get(name) {
            loadTemplate(name, true, runtimeTemplateFolder)
        }
    } else {
        // try to get from cache or load unpopulated template
        (templateCache.getIfPresent(name) ?: loadTemplate(
            name,
            false,
            runtimeTemplateFolder
        )).let {
            // reset populated template, in case it was loaded from cache
            VcTemplate(it.name, null, it.mutable)
        }
    }

    fun getTemplate(
        name: String,
        populateTemplate: Boolean = true,
        runtimeTemplateFolder: String = "/vc-templates-runtime"
    ): VcTemplate {
        val cachedTemplate = retrieveOrLoadCachedTemplate(name, populateTemplate, runtimeTemplateFolder)

        return if (cachedTemplate.template == null && populateTemplate) {
            templateCache.invalidate(name)
            retrieveOrLoadCachedTemplate(name, true, runtimeTemplateFolder)
        } else cachedTemplate
    }


    private val resourceWalk = lazy {

        val resource = javaClass.getResource(resourcePath)
        checkNotNull(resource) { "Cannot find resource path: $resourcePath" }

        log.debug { "Loading templates from: $resource" }

        when {
            File(resource.file).isDirectory ->
                File(resource.file).walk().filter { it.isFile }.map {
                    (it.relativeTo(File(resource.file)).parent?.let { "$it/" } ?: "") + it.nameWithoutExtension
                }.toList()

            else -> {
                FileSystems.newFileSystem(resource.toURI(), emptyMap<String, String>()).use { fs ->
                    Files.walk(fs.getPath(resourcePath))
                        .filter { it.isRegularFile() }
                        .map {
                            (Path.of(it.toString()).relativeTo(Path.of(resourcePath)).parent?.let { "$it/" } ?: "") + it.nameWithoutExtension
                        }.toList()
                }
            }
        }
    }

    private fun listResources(): List<String> = resourceWalk.value

    private fun listRuntimeTemplates(folderPath: String): List<String> {
        val templatesFolder = File(folderPath)
        return if (templatesFolder.isDirectory) {
            templatesFolder.walk().filter { it.isFile }.map {
                (it.relativeTo(templatesFolder).parent?.let { "$it/" } ?: "") + it.nameWithoutExtension
            }.toList()
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
