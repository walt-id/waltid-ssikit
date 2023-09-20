package id.walt.services.key.deriver

import java.security.PublicKey

interface PublicKeyDeriver<T> {
    fun derive(key: T): PublicKey?
}
