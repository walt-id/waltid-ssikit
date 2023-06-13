package id.walt.services.did.factories

import id.walt.crypto.Key
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.did.DidEbsi
import id.walt.services.did.DidEbsiCreateOptions
import id.walt.services.did.DidOptions
import id.walt.services.did.composers.DidDocumentComposer
import id.walt.services.did.composers.models.DocumentComposerJwkParameter
import id.walt.services.did.composers.models.DocumentComposerKeyJwkParameter
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType

class DidEbsiFactory(
    private val keyService: KeyService,
    private val documentV1Composer: DidDocumentComposer<DidEbsi>,
    private val documentV2Composer: DidDocumentComposer<DidEbsi>,
) : DidFactory {
    override fun create(key: Key, options: DidOptions?): Did {
        return when ((options as? DidEbsiCreateOptions)?.version ?: 1) {
            1 -> createDidEbsiV1(key)
            2 -> createDidEbsiV2(key)
            else -> throw Exception("Did ebsi version must be 1 or 2")
        }
    }

    private fun createDidEbsiV1(key: Key) = documentV1Composer.make(
        DocumentComposerKeyJwkParameter(
            DidUrl.generateDidEbsiV1DidUrl(), keyService.toJwk(key.keyId.id), key
        )
    )

    private fun createDidEbsiV2(key: Key) = let {
        val publicKeyJwk = keyService.toJwk(key.keyId.id, KeyType.PUBLIC)
        val publicKeyThumbprint = publicKeyJwk.computeThumbprint()
        documentV2Composer.make(
            DocumentComposerJwkParameter(
                DidUrl.generateDidEbsiV2DidUrl(publicKeyThumbprint.decode()), publicKeyJwk
            )
        )
    }
}
