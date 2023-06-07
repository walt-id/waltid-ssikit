package id.walt.credentials.w3c.templates

import id.walt.credentials.w3c.VerifiableCredential

@Deprecated("Deprecated in favor of VcTemplateService", ReplaceWith("VcTemplateService"))
object VcTemplateManager {

    private val templateService get() = VcTemplateService.getService()

    fun register(name: String, template: VerifiableCredential): VcTemplate {
        return templateService.register(name, template)
    }

    fun getTemplate(
        name: String,
        loadTemplate: Boolean = true,
        runtimeTemplateFolder: String = "/vc-templates-runtime"
    ): VcTemplate {
        return templateService.getTemplate(name, loadTemplate, runtimeTemplateFolder)
    }


    fun listTemplates(runtimeTemplateFolder: String = "/vc-templates-runtime"): List<VcTemplate> {
        return templateService.listTemplates(runtimeTemplateFolder)
    }

    fun unregisterTemplate(name: String) {
        return templateService.unregisterTemplate(name)
    }
}
