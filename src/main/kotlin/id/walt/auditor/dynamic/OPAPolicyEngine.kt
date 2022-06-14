package id.walt.auditor.dynamic

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import id.walt.common.resolveContentToFile
import mu.KotlinLogging
import java.io.File

object OPAPolicyEngine : PolicyEngine {
    private val log = KotlinLogging.logger {}

    const val TEMP_PREFIX = "_TEMP_"

    fun validate(jsonInput: String, data: Map<String, Any?>, regoPolicy: String, regoQuery: String): Boolean {
        val input: Map<String, Any?> = Parser.default().parse(StringBuilder(jsonInput)) as JsonObject

        return validate(input, data, regoPolicy, regoQuery)
    }

    override fun validate(input: Map<String, Any?>, data: Map<String, Any?>, policy: String, query: String): Boolean {
        val regoFile = resolveContentToFile(policy, tempPrefix = TEMP_PREFIX, tempPostfix = ".rego")
        val dataFile = File.createTempFile("data", ".json")
        dataFile.writeText(JsonObject(data).toJsonString())
        try {
            val p = ProcessBuilder("opa", "eval", "-d", regoFile.absolutePath, "-d", dataFile.absolutePath, "-I", "-f", "values", query)
                .start()
            p.outputStream.writer().use { it.write(JsonObject(input).toJsonString()) }
            val output = p.inputStream.reader().use { it.readText() }
            p.waitFor()
            log.debug("rego eval output: {}", output)
            return Klaxon().parseArray<Boolean>(output)?.all { it } ?: false
        } finally {
            if(regoFile.exists() && regoFile.name.startsWith(TEMP_PREFIX)) {
              regoFile.delete()
            }
            if(dataFile.exists()) {
                dataFile.delete()
            }
        }
    }

    override val type: PolicyEngineType = PolicyEngineType.OPA
}
