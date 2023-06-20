package id.walt.services.did

sealed class DidOptions

data class DidWebCreateOptions(val domain: String?, val path: String? = null) : DidOptions()
data class DidEbsiCreateOptions(val version: Int) : DidOptions()
data class DidCheqdCreateOptions(val network: String) : DidOptions()
data class DidKeyCreateOptions(val useJwkJcsPub: Boolean) : DidOptions()

data class DidEbsiResolveOptions(val isRaw: Boolean) : DidOptions()
