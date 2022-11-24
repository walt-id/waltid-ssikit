package id.walt.credentials.w3c.templates

import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.services.context.ContextManager
import id.walt.services.hkvstore.FileSystemHKVStore
import id.walt.services.hkvstore.HKVKey
import java.io.File

object VcTemplateManager {
    const val SAVED_VC_TEMPLATES_KEY = "vc-templates"

    fun register(name: String, template: String) {
        ContextManager.hkvStore.put(HKVKey(SAVED_VC_TEMPLATES_KEY, name), template)
    }

    fun getTemplate(name: String): String {
        return ContextManager.hkvStore.getAsString(HKVKey(SAVED_VC_TEMPLATES_KEY, name)) ?:
        object {}.javaClass.getResource("/vc-templates/$name.json")?.readText() ?:
        throw Exception("No template found, with name $name")
    }

    fun listTemplates(): List<String> {
      val resourceDir = File(object {}.javaClass.getResource("/vc-templates").file)
      return resourceDir.walk().filter { it.isFile }.map { it.nameWithoutExtension }.plus(
        ContextManager.hkvStore.listChildKeys(HKVKey(SAVED_VC_TEMPLATES_KEY), false).map { it.name }
      ).toSet().toList()
    }

    fun loadTemplate(name: String) = getTemplate(name).toVerifiableCredential()

    fun unregisterTemplate(name: String) {
        ContextManager.hkvStore.delete(HKVKey(SAVED_VC_TEMPLATES_KEY, name))
    }
}
