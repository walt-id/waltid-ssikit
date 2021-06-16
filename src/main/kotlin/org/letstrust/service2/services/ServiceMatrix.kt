package org.letstrust.service2.services

import org.letstrust.service2.BaseService
import org.letstrust.service2.ServiceRegistry
import java.io.File
import java.util.*
import kotlin.reflect.KClass

class ServiceMatrix(filePath: String) {
    private val serviceList = HashMap<String, String>()

    init {
        val properties = Properties().apply {
            load(File(filePath).reader())
        }

        properties.entries.forEach {
            serviceList[it.value.toString()] = it.key.toString()
        }

        serviceList.forEach { (implementationClass, serviceClass) ->
            try {
                val implementation: BaseService =
                    Class.forName(implementationClass).getDeclaredConstructor().newInstance() as BaseService
                val service: KClass<BaseService> = (Class.forName(serviceClass) as Class<BaseService>).kotlin
                ServiceRegistry.registerService(implementation, service)
            } catch (e: InstantiationException) {
                throw InstantiationException("ServiceMatrix: Failed to initialize implementation \"$implementationClass\" for \"$serviceClass\"!")
            }
        }
    }
}
