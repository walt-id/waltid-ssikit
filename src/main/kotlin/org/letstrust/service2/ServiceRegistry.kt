package org.letstrust.service2

import kotlin.reflect.KClass

abstract class BaseService {
    protected open val implementation: BaseService get() = throw NotImplementedError()
}

object ServiceRegistry {
    val services = HashMap<KClass<out BaseService>, BaseService>()

    inline fun <reified T : BaseService> registerService(serviceImplementation: BaseService) {
        services[T::class] = serviceImplementation
    }

    inline fun <reified Service : BaseService> getService(): Service = (services[Service::class] as Service)
}
