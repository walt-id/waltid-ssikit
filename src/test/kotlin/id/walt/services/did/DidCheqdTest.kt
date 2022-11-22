package id.walt.services.did

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DidCheqdTest : StringSpec({
        "Test DID CHEQD resolving" {
            val did = DidService.resolve("did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY")
            println(did.id)
            did.id shouldBe "did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY"
        }
    }
)
