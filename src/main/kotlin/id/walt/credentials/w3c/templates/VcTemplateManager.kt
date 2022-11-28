package id.walt.credentials.w3c.templates

import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.services.context.ContextManager
import id.walt.services.hkvstore.FileSystemHKVStore
import id.walt.services.hkvstore.HKVKey
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk
import kotlin.jvm.internal.Reflection
import kotlin.streams.toList

object VcTemplateManager {
  const val SAVED_VC_TEMPLATES_KEY = "vc-templates"

  fun register(name: String, template: String): VcTemplate {
    ContextManager.hkvStore.put(HKVKey(SAVED_VC_TEMPLATES_KEY, name), template)
    return VcTemplate(name, template, true)
  }

  fun getTemplate(name: String): VcTemplate {
    return ContextManager.hkvStore.getAsString(HKVKey(SAVED_VC_TEMPLATES_KEY, name))?.let { VcTemplate(name, it, true) }
      ?: object {}.javaClass.getResource("/vc-templates/$name.json")?.readText()?.let { VcTemplate(name, it, false) }
      ?: throw Exception("No template found, with name $name")
  }

  private fun listResources(resourcePath: String): List<String> {
    val resource = object {}.javaClass.getResource(resourcePath)
    return if(File(resource.file).isDirectory) {
      File(resource.file).walk().filter { it.isFile }.map { it.nameWithoutExtension }.toList()
    } else {
      FileSystems.newFileSystem(resource.toURI(), emptyMap<String, String>()).use { fs ->
        Files.walk(fs.getPath(resourcePath))
          .filter { it.isRegularFile() }
          .map { it.nameWithoutExtension }.toList()
      }
    }
  }

  fun listTemplates(): List<String> {
    return listResources("/vc-templates").plus(
      ContextManager.hkvStore.listChildKeys(HKVKey(SAVED_VC_TEMPLATES_KEY), false).map { it.name }
    ).toSet().toList()
  }

  fun getAllTemplates(): List<VcTemplate> {
    return listTemplates().map { getTemplate(it) }
  }

  fun loadTemplate(name: String) = getTemplate(name).template.toVerifiableCredential()

  fun unregisterTemplate(name: String) {
    ContextManager.hkvStore.delete(HKVKey(SAVED_VC_TEMPLATES_KEY, name))
  }
}
