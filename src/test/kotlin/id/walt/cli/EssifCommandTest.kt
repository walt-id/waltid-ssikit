package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.crypto.JwtUtils.getJwtExpirationMessageIfExpired
import id.walt.crypto.JwtUtils.isJwtExpired
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.did.DidEbsiCreateOptions
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.TrustedIssuerClient
import id.walt.services.ecosystems.essif.timestamp.Timestamp
import id.walt.services.ecosystems.essif.timestamp.WaltIdTimestampService
import io.kotest.assertions.retry
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.mpp.log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


class EssifCommandTest : StringSpec({

    val bearerTokenExpiredMessage = "YOUR EBSI BEARER TOKEN HAS EXPIRED. GET A NEW EBSI BEARER TOKEN FROM ${TrustedIssuerClient.onboarding}"
    val bearerToken = File("data/ebsi/bearer-token.txt")
    val enableTests = bearerToken.exists() && if (!isJwtExpired(bearerToken.readText())) true else {
        println(bearerTokenExpiredMessage)
        println(getJwtExpirationMessageIfExpired(bearerToken.readText()))
        false
    }

    ServiceMatrix("service-matrix.properties")

    lateinit var keyId: String
    lateinit var did: String
    lateinit var identifier: String

    "META - Check EBSI Bearer Token".config(enabled = enableTests) {
        if (isJwtExpired(bearerToken.readText())) {
            runBlocking {
                repeat(10) {
                    println(bearerTokenExpiredMessage)
                    delay(1000)
                }
            }
        }
    }

    "0. Create EBSI DID" {
        keyId = CryptoService.getService().generateKey(KeyAlgorithm.ECDSA_Secp256k1).id
        did = DidService.create(DidMethod.ebsi, keyId, DidEbsiCreateOptions(1))
        identifier = did.removePrefix("did:ebsi:")
    }

    "1. onboard --help" {
        val e = shouldThrow<PrintHelpMessage> {
            EssifOnboardingCommand().parse(listOf("--help"))
        }
        val message = e.context?.command?.getFormattedHelp()
        message shouldContain "bearer-token-file"
        message shouldContain "--did"
    }

    /**
     * Before running the following tests a valid bearer token needs to be place in file data/ebsi/bearer-token.txt.
     * The token can be retrieved from https://app-pilot.ebsi.eu/users-onboarding/v2/
     */
    "2. onboard --did".config(enabled = enableTests) {
        if (!bearerToken.exists()) throw NoSuchElementException("Bearer Token from ${TrustedIssuerClient.onboarding} should be placed in file data/ebsi/bearer-token.txt")

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
        retry(9, 2.minutes, delay = 4.seconds) {
            println("Registering did")
            shouldNotThrowAny {
                EssifDidRegisterCommand().parse(listOf("--did", did, "--eth-key", keyId))
            }
        }
    }

    var transactionHash: String? = null
    var timestamp: Timestamp? = null

    fun validateTimestamp(timestampToCheck: Timestamp?) {
        println("Validating timestamp: $timestampToCheck")

        timestamp shouldNotBe null
        timestampToCheck shouldNotBe null
        timestampToCheck!!.transactionHash shouldBe transactionHash
        timestampToCheck.timestampId shouldBe timestamp!!.timestampId
        timestampToCheck.hash shouldBe timestamp!!.hash
        timestampToCheck.timestampedBy shouldBe timestamp!!.timestampedBy
    }

    "5. Insert timestamp".config(enabled = enableTests) {
        retry(9, 2.minutes, delay = 4.seconds) {
            println("Inserting timestamp.")
            shouldNotThrowAny {

                /*EssifTimestampCreateCommand().parse(
                    listOf(
                        "--did",
                        did,
                        "--eth-key",
                        ethKey.id,
                        "${RESOURCES_PATH}/ebsi/test-data.json"
                    )
                )*/

                transactionHash =
                    WaltIdTimestampService().createTimestamp(did, keyId, "{\"test\": \"${UUID.randomUUID()}\"}")
                log { "ESSIFCOMMANDTEST: $transactionHash" }
                transactionHash.shouldNotBeEmpty()
                transactionHash.shouldNotBeBlank()
                Thread.sleep(5000) // wait for timestamp to be public
            }
        }
    }

    "6. Get timestamp transaction hash".config(enabled = false) {
        timestamp =
            WaltIdTimestampService().getByTransactionHash(transactionHash!!)
        validateTimestamp(timestamp)

        WaltIdTimestampService().getByTransactionHash("do not exist") shouldBe null

        EssifTimestampGetCommand().parse(
            listOf(
                "--timestamp-txhash",
                transactionHash!!
            )
        )
    }

    "7. Get by timestamp Id".config(enabled = false) {
        val timestampReceived =
            WaltIdTimestampService().getByTimestampId(timestamp!!.timestampId!!)
        validateTimestamp(timestampReceived)
        EssifTimestampGetCommand().parse(listOf("--timestamp-id", timestamp!!.timestampId!!))
    }

    "8. essif tir get -r" {
        EssifTirGetIssuerCommand().parse(listOf("--did", did, "-r"))
    }

    // TODO: ESSIF backend issue
    "9. essif tir get -t".config(enabled = enableTests) {
        EssifTirGetIssuerCommand().parse(listOf("--did", did, "-t"))
    }
})
