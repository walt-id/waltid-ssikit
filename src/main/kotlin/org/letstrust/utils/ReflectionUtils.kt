package org.letstrust.utils

/**
 * General reflection utilities used by letstrust-ssi-core
 */
object ReflectionUtils {
    /**
     * @return KClass of the specified class by full name
     */
    fun getKClassByName(name: String) = Class.forName(name).kotlin
}
