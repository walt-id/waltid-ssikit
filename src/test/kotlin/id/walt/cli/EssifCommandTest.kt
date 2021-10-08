package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.hkvstore.HKVKey
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.key.KeyService
import io.kotest.assertions.retry
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds


// CLI KIT Options

//try {
//    EssifOnboardingCommand().parse(listOf<String>("-h"))
//} catch (e: ProgramResult) {
//    println(e.statusCode)
//} catch (e: PrintHelpMessage) {
//    println(e.command.getFormattedHelp())
//} catch (e: PrintCompletionMessage) {
//    println(e.message)
//} catch (e: PrintMessage) {
//    println(e.message)
//} catch (e: UsageError) {
//    println(e.helpMessage())
//} catch (e: CliktError) {
//    println(e.message)
//} catch (e: Abort) {
//    println(e.message)
//}


@OptIn(ExperimentalTime::class)
class EssifCommandTest : StringSpec({

    val bearerToken = File("data/ebsi/bearer-token.txt")
    val enableTests = bearerToken.exists()

    ServiceMatrix("service-matrix.properties")

    // DID used for onboarding
    val key = KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519)
    val ethKey = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
    var did = DidService.create(DidMethod.ebsi, keyAlias = key.id)
    val identifier = DidUrl.from(did).identifier

    "onboard --help" {
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
    "onboard --did".config(enabled = enableTests) {
        if (!bearerToken.exists()) throw Exception("Bearer Token from https://app.preprod.ebsi.eu/users-onboarding/ should be placed in file data/ebsi/bearer-token.txt")

        println("Generating verifiable authorization...")
        EssifOnboardingCommand().parse(listOf("--did", did, File("data/ebsi/bearer-token.txt").absolutePath))
        File("data/ebsi/${identifier}/verifiable-authorization.json").exists() shouldBe true
    }

    "auth-api --did".config(enabled = enableTests) {
        println("Starting auth...")
        EssifAuthCommand().parse(listOf("--did", did))
        File("data/ebsi/${identifier}/ebsi_access_token.json").exists() shouldBe true
        File("data/ebsi/${identifier}/ake1_enc.json").exists() shouldBe true
    }

    "did register --did".config(enabled = enableTests) {
        retry(9, 2.minutes, delay = 4.seconds) {
            println("Registering did")
            shouldNotThrowAny {
                EssifDidRegisterCommand().parse(listOf("--did", did, "--eth-key", ethKey.id))
            }
        }
        HKVStoreService.getService().delete(HKVKey("ebsi", identifier), true)
    }

    "essif tir get -r" {
        EssifTirGetIssuerCommand().parse(listOf("--did", "did:ebsi:224AEY73SGS1gpTvbt5TNTTPdNj8GU6NAq2AVBFmasQbntCt", "-r"))
    }

    "essif tir get -t" {
        EssifTirGetIssuerCommand().parse(listOf("--did", "did:ebsi:224AEY73SGS1gpTvbt5TNTTPdNj8GU6NAq2AVBFmasQbntCt", "-t"))
    }
})
