package id.walt.rest.core.requests.did

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import id.walt.model.DidMethod
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
@TypeFor(field = "method", adapter = CreateDidRequestMethodAdapter::class)
sealed class CreateDidRequest(
    val method: String,
){
    abstract val keyAlias: String?
}

class CreateDidRequestMethodAdapter : TypeAdapter<CreateDidRequest> {
    override fun classFor(type: Any): KClass<out CreateDidRequest> = when(DidMethod.valueOf(type as String)){
        DidMethod.key -> KeyCreateDidRequest::class
        DidMethod.web -> WebCreateDidRequest::class
        DidMethod.ebsi -> EbsiCreateDidRequest::class
        DidMethod.iota -> IotaCreateDidRequest::class
        DidMethod.jwk -> JWKCreateDidRequest::class
        DidMethod.cheqd -> CheqdCreateDidRequest::class
    }
}


@Serializable
class KeyCreateDidRequest(
    override val keyAlias: String? = null,
    val useJwkJcsPub: Boolean = false,
) : CreateDidRequest("key")
@Serializable
class WebCreateDidRequest(
    val domain: String? = null,
    val path: String? = null,
    override val keyAlias: String? = null,
) : CreateDidRequest("web")
@Serializable
class EbsiCreateDidRequest(
    val version: Int = 1,
    override val keyAlias: String? = null,
) : CreateDidRequest("ebsi")
@Serializable
class IotaCreateDidRequest(override val keyAlias: String? = null) : CreateDidRequest("iota")
@Serializable
class JWKCreateDidRequest(override val keyAlias: String? = null) : CreateDidRequest("jwk")
@Serializable
class CheqdCreateDidRequest(
    val network: String = "testnet",
    override val keyAlias: String? = null,
) : CreateDidRequest("cheqd")

