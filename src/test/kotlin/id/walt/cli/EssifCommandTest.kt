package id.walt.cli

import com.github.ajalt.clikt.core.PrintHelpMessage
import id.walt.servicematrix.ServiceMatrix
import io.kotest.assertions.retry
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import id.walt.cli.EssifAuthCommand
import id.walt.cli.EssifDidRegisterCommand
import id.walt.cli.EssifOnboardingCommand
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import java.io.File
import kotlin.time.Duration
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
    val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
    var did = DidService.create(DidMethod.ebsi, keyAlias = key.id)

    "onboard --help" {
        val e = shouldThrow<PrintHelpMessage> {
            EssifOnboardingCommand().parse(listOf("--help"))
        }
        val message = e.command.getFormattedHelp()
        println(message)
        message shouldContain "-k, --key-id"
        message shouldContain "-d, --did"
    }

    /**
     * Before running the following tests a valid bearer token needs to be place in file data/ebsi/bearer-token.txt.
     * The token can be retrieved from https://app.preprod.ebsi.eu/users-onboarding/
     */
    "onboard --did".config(enabled = enableTests) {
        if (!bearerToken.exists()) throw Exception("Bearer Token from https://app.preprod.ebsi.eu/users-onboarding/ should be placed in file data/ebsi/bearer-token.txt")

        println("Generating verifiable authorization...")
        File("data/ebsi/verifiable-authorization.json").delete()
        EssifOnboardingCommand().parse(listOf("--did", did))
        File("data/ebsi/verifiable-authorization.json").exists() shouldBe true
    }

    "auth-api --did".config(enabled = enableTests) {
        println("Starting auth...")
        File("data/ebsi/ebsi_access_tokenjson").delete()
        File("data/ebsi/ake1_enc.json").delete()
        EssifAuthCommand().parse(listOf("--did", did))
        File("data/ebsi/ebsi_access_token.json").exists() shouldBe true
        File("data/ebsi/ake1_enc.json").exists() shouldBe true
    }

    "did register --did".config(enabled = enableTests) {
        retry(9, 2.minutes, delay = 4.seconds) {
            println("Registering did")
            shouldNotThrowAny {
                EssifDidRegisterCommand().parse(listOf("--did", did))
            }
        }
    }
})
