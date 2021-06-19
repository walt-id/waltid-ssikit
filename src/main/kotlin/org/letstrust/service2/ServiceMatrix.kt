package org.letstrust.service2

import org.letstrust.utils.ReflectionUtils.getKClassByName
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Manager to load service definitions from Java property files and subsequently
 * register these service mappings in the [ServiceRegistry].
 *
 * The primary (no-arg) constructor allows you to call [loadServiceDefinitions] yourself. Register
 * the definitions using [registerServiceDefinitions].
 * The secondary (string-arg) constructor automatically loads the service definitions and
 * registers them in the [ServiceRegistry].
 *
 * @constructor The primary (no-arg) constructor allows you to call [loadServiceDefinitions] yourself.
 */
class ServiceMatrix() {
    private val serviceList = HashMap<String, String>()

    /**
     * Uses the Java property class to read a properties file containing the service mappings.
     *
     * @param filePath The path to the service definition file as String
     */
    fun loadServiceDefinitions(filePath: String) = Properties().apply {
        load(File(filePath).reader())
        serviceList.putAll(entries.associate { it.value.toString() to it.key.toString() })
    }

    /**
     * Registers all loaded service definitions in the [ServiceRegistry].
     */
    fun registerServiceDefinitions() {
        serviceList.forEach { (implementationClass, serviceClass) ->
            try {
                val implementation: BaseService = getKClassByName(implementationClass).createInstance() as BaseService
                val service: KClass<out BaseService> = getKClassByName(serviceClass) as KClass<out BaseService>

                ServiceRegistry.registerService(implementation, service)
            } catch (e: InstantiationException) {
                throw InstantiationException("ServiceMatrix: Failed to initialize implementation \"$implementationClass\" for \"$serviceClass\"!")
            }
        }
    }

    /**
     * Calling this constructor will automatically load the service definitions from [filePath] and registers them
     *
     * @param filePath The path to the service definition file as String
     */
    constructor(filePath: String) : this() {
        loadServiceDefinitions(filePath)
        registerServiceDefinitions()
    }
}
