package org.letstrust.service2

import kotlin.reflect.KClass

object LetstrustServiceRegistry {
    val services = HashMap<KClass<out LetstrustService>, LetstrustService>()

    inline fun <reified T : LetstrustService> registerService(serviceImplementation: T) {
        services[T::class] = serviceImplementation
    }

    inline fun <reified T : LetstrustService> getService() = services[T::class]!!
}
