package id.walt.services.did.composers

import com.beust.klaxon.Klaxon
import id.walt.model.DidUrl
import id.walt.model.VerificationMethod
import id.walt.model.did.DidEbsi
import id.walt.services.did.composers.models.DocumentComposerBaseParameter
import id.walt.services.did.composers.models.DocumentComposerJwkParameter

class DidEbsiV2DocumentComposer : DidDocumentComposerBase<DidEbsi>() {
    override fun make(parameter: DocumentComposerBaseParameter): DidEbsi =
        (parameter as? DocumentComposerJwkParameter)?.let {
            val vmId = "${it.didUrl.did}#${it.jwk.computeThumbprint()}"
            if (DidUrl.generateDidEbsiV2DidUrl(it.jwk.computeThumbprint().decode()).identifier != it.didUrl.identifier) {
                throw IllegalArgumentException("Public key doesn't match with DID identifier")
            }
            DidEbsi(
                context = listOf("https://w3id.org/did/v1"),
                id = it.didUrl.did,
                verificationMethod = listOf(
                    VerificationMethod(
                        id = vmId,
                        type = "JsonWebKey2020",
                        controller = it.didUrl.did,
                        publicKeyJwk = Klaxon().parse(it.jwk.toJSONString())
                    )
                ),
                authentication = listOf(VerificationMethod.Reference(vmId)),
                assertionMethod = listOf(VerificationMethod.Reference(vmId))
            )
        } ?: throw IllegalArgumentException("Couldn't parse ebsi-v2 document composer parameter")
}
