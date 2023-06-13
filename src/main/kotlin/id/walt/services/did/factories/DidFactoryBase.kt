package id.walt.services.did.factories

import id.walt.model.DidMethod
import id.walt.services.did.composers.*
import id.walt.services.key.KeyService

abstract class DidFactoryBase : DidFactory {
    companion object {
//        private val didKeyDocumentComposer = DidKeyDocumentComposer()
        private val didJwkDocumentComposer = DidJwkDocumentComposer()
        private val didWebDocumentComposer = DidWebDocumentComposer()
        private val didEbsiV1DocumentComposer = DidEbsiV1DocumentComposer()
        private val didEbsiV2DocumentComposer = DidEbsiV2DocumentComposer()

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        fun new(method: DidMethod, keyService: KeyService): DidFactory = when (method) {
            DidMethod.iota -> DidIotaFactory()
            DidMethod.cheqd -> DidCheqdFactory()
            // TODO: remove key-service dependency and cache composer similar others
            DidMethod.key -> DidKeyFactory(keyService, DidKeyDocumentComposer(keyService))
            DidMethod.web -> DidWebFactory(keyService, didWebDocumentComposer)
            DidMethod.ebsi -> DidEbsiFactory(keyService, didEbsiV1DocumentComposer, didEbsiV2DocumentComposer)
            DidMethod.jwk -> DidJwkFactory(keyService, didJwkDocumentComposer)
            else -> throw UnsupportedOperationException("DID method $method not supported")
        }
    }
}
