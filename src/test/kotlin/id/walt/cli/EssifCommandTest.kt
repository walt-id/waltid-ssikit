package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.essif.timestamp.Timestamp
import id.walt.services.essif.timestamp.WaltIdTimestampService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.retry
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldNotBeEmpty
import java.io.File
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class EssifCommandTest : StringSpec({

    val bearerToken = File("data/ebsi/bearer-token.txt")
    val enableTests = bearerToken.exists()

    ServiceMatrix("service-matrix.properties")

    // DID used for onboarding
    val key = KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519)
    val ethKey = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
    val did = DidService.create(DidMethod.ebsi, keyAlias = key.id)
    val identifier = DidUrl.from(did).identifier

    "1. onboard --help" {
        val e = shouldThrow<PrintHelpMessage> {
            EssifOnboardingCommand().parse(listOf("--help"))
        }
        val message = e.command.getFormattedHelp()
        println(message)
        message shouldContain "BEARER-TOKEN-FILE"
        message shouldContain "-d, --did"
    }

    /**
     * Before running the following tests a valid bearer token needs to be place in file data/ebsi/bearer-token.txt.
     * The token can be retrieved from https://app.preprod.ebsi.eu/users-onboarding/
     */
    "2. onboard --did".config(enabled = enableTests) {
        if (!bearerToken.exists()) throw Exception("Bearer Token from https://app.preprod.ebsi.eu/users-onboarding/ should be placed in file data/ebsi/bearer-token.txt")

        println("Generating verifiable authorization...")
        EssifOnboardingCommand().parse(listOf("--did", did, File("data/ebsi/bearer-token.txt").absolutePath))
        File("data/ebsi/${identifier}/verifiable-authorization.json").exists() shouldBe true
    }

    "3. auth-api --did".config(enabled = enableTests) {
        println("Starting auth...")
        EssifAuthCommand().parse(listOf("--did", did))
        File("data/ebsi/${identifier}/ebsi_access_token.json").exists() shouldBe true
        File("data/ebsi/${identifier}/ake1_enc.json").exists() shouldBe true
    }

    "4. did register --did".config(enabled = enableTests) {
        retry(9, Duration.minutes(2), delay = Duration.seconds(4)) {
            println("Registering did")
            shouldNotThrowAny {
                EssifDidRegisterCommand().parse(listOf("--did", did, "--eth-key", ethKey.id))
            }
        }
    }

    var transactionHash: String? = null
    "5. Insert timestamp".config(enabled = enableTests) {
        retry(9, Duration.minutes(2), delay = Duration.seconds(4)) {
            println("Inserting timestamp.")
            shouldNotThrowAny {

                EssifTimestampCreateCommand().parse(listOf("--did", did, "--eth-key", ethKey.id, "${RESOURCES_PATH}/ebsi/test-data.json"))

                transactionHash = WaltIdTimestampService().createTimestamp(did, ethKey.id, "{\"test\": \"${UUID.randomUUID()}\"}")
                transactionHash.shouldNotBeEmpty()
                transactionHash.shouldNotBeBlank()
            }
        }
    }

    "6. Get timestamp transaction hash".config(enabled = false) {
        val timestamp = WaltIdTimestampService().getByTransactionHash("0x45680f0a1d2b54d5abe785a93b90e42ee1d37aa0a4c03ff2d07d5ac431232674"/*transactionHash!!*/)
        validateTimestamp(timestamp)

        WaltIdTimestampService().getByTransactionHash("do not exist") shouldBe null

        EssifTimestampGetCommand().parse(listOf("--timestamp-txhash", "0x45680f0a1d2b54d5abe785a93b90e42ee1d37aa0a4c03ff2d07d5ac431232674"))
    }

    "7. Get by timestamp Id".config(enabled = false) {
        val timestamp = WaltIdTimestampService().getByTimestampId("uEiCHMUGYdJ6Lu8ugrCaEymIUAq6kUJHq10clWEcDvUwHLQ"/*timestampId!!*/)
        validateTimestamp(timestamp)
        EssifTimestampGetCommand().parse(listOf("--timestamp-id", "uEiCHMUGYdJ6Lu8ugrCaEymIUAq6kUJHq10clWEcDvUwHLQ"))
    }

    // TODO: ESSIF backend issue
    "8. essif tir get -r".config(enabled = false) {
        EssifTirGetIssuerCommand().parse(listOf("--did", "did:ebsi:224AEY73SGS1gpTvbt5TNTTPdNj8GU6NAq2AVBFmasQbntCt", "-r"))
    }

    // TODO: ESSIF backend issue
    "9. essif tir get -t".config(enabled = false) {
        EssifTirGetIssuerCommand().parse(listOf("--did", "did:ebsi:224AEY73SGS1gpTvbt5TNTTPdNj8GU6NAq2AVBFmasQbntCt", "-t"))
    }
})

private fun validateTimestamp(timestamp: Timestamp?) {
    println("Validating timestamp: $timestamp")

    timestamp shouldNotBe null
    timestamp!!.timestampId shouldBe "uEiCHMUGYdJ6Lu8ugrCaEymIUAq6kUJHq10clWEcDvUwHLQ"
    timestamp!!.hash shouldBe "mEiACz5o3HeOXrLZnpzc1vJSuaYO31XV1PqaESJObOqdFBw"
    timestamp!!.transactionHash shouldBe "0x45680f0a1d2b54d5abe785a93b90e42ee1d37aa0a4c03ff2d07d5ac431232674"
    timestamp!!.timestampedBy shouldBe "0xD39F93C93E0B9153d4b09B8263A3e553eaf6d2e0"
}
