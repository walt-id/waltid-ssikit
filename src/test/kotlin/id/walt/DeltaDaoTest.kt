package id.walt

import deltadao.DeltaDao
import id.walt.auditor.Auditor
import id.walt.auditor.PolicyRegistry
import id.walt.cli.logic.KeyCommandLogic
import id.walt.cli.resolveDidHelper
import id.walt.crypto.KeyId
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.essif.EssifClient
import id.walt.signatory.*
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.templates.VcTemplateManager
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.io.File
import java.nio.file.Path
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.stream.Collectors
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.readText

@Ignored
class DeltaDaoTest : StringSpec({
    if (Path("data/bearer-token.txt").exists())
        Path("data/bearer-token.txt").moveTo(Path("bearer-token.txt"))

    println("Dir was deleted: " + File("data").deleteRecursively())

    println("Registering services")
    ServiceMatrix("service-matrix.properties")

    DeltaDao.registerDeltaDaoCredentials()

    DeltaDao.registerDeltaDaoDataProvider()

    if (Path("bearer-token.txt").exists())
        Path("bearer-token.txt").moveTo(Path("data/bearer-token.txt"))

    println("Checking for bearer token...")
    while (!File("data/bearer-token.txt").exists()) {
        println("https://app.preprod.ebsi.eu/users-onboarding")
        println(File("data/bearer-token.txt").absolutePath)
        Thread.sleep(1000)
    }

    lateinit var keyId: KeyId
    "2.1 Key generation" {
        val algorithm = "Secp256k1"

        println("Generating $algorithm key pair...")

        keyId = KeyCommandLogic.genKey(algorithm)

        println("Key \"$keyId\" generated.")
    }

    lateinit var didEbsi: String
    "2.2 DID EBSI creation" {
        println("Creating did...")
        didEbsi = DidService.create(DidMethod.valueOf("ebsi"), keyId.id)
        didEbsi shouldStartWith "did:ebsi:"
        println("DID created: $didEbsi")
    }

    "3.1 Onboarding flow:" {
        shouldNotThrowAny {
            val res = EssifClient.onboard(didEbsi, File("data/bearer-token.txt").readText().replace("\n", ""))
            println(res)
        }
    }

    "3.2 Authentication API flow:" {
        shouldNotThrowAny {
            EssifClient.authApi(didEbsi)
        }
        println("done")
    }

    "3.3 Writing to ledger (signing of ETH transaction):"  {
        shouldNotThrowAny {
            EssifClient.registerDid(didEbsi, didEbsi)
        }
    }

    "3.4 Try out DID resolving via CLI:" {
        println("RESOLVING: $didEbsi")
        lateinit var res: String
        shouldNotThrowAny {
            res = resolveDidHelper(didEbsi, false)
        }
        println(res)
        res shouldContain "{"
        println("Also try https://api.preprod.ebsi.eu/docs/?urls.primaryName=DID%20Registry%20API#/DID%20Registry/get-did-registry-v2-identifier")
    }

    lateinit var didKey: String
    "4.1. Creating a DID for the recipient (holder) of the credential" {
        didKey = DidService.create(DidMethod.valueOf("key"), null)
        didKey shouldStartWith "did:key:"
        println("Generated: $didKey")
    }

    "4.2. Issuing W3C Verifiable Credential" {
        val interactive = true
        val template = "GaiaxCredential"
        val issuerDid = didEbsi
        val subjectDid = didKey
        val dest = File("data/vc.json")

        if (interactive) {
            val cliDataProvider = CLIDataProviders.getCLIDataProviderFor(template)
            if (cliDataProvider == null) {
                println("No interactive data provider available for template: $template")
                error("err")
            }
            val templ = VcTemplateManager.loadTemplate(template)
            DataProviderRegistry.register(templ::class, cliDataProvider)
        }
        println("Issuing a verifiable credential (using template ${template})...")

        // Loading VC template

        val vcStr = Signatory.getService().issue(
            template,
            ProofConfig(issuerDid, subjectDid, "Ed25519Signature2018", null, ProofType.LD_PROOF)
        )


        println("\nResults:\n")

        println("Issuer \"$issuerDid\"")
        println("⇓ issued a \"$template\" to ⇓")
        println("Holder \"$subjectDid\"")

        println("\nCredential document (below, JSON):\n\n$vcStr")

        dest.run {
            dest.writeText(vcStr)
            println("\nSaved credential to file: $dest")
        }

        dest.readText() shouldContain "proof"
    }

    lateinit var vpFileName: String
    "4.3. Creating a W3C Verifiable Presentation" {
        val src: List<Path> = listOf(Path("data/vc.json"))
        val holderDid: String = didKey



        println("Creating a verifiable presentation for DID \"$holderDid\"...")
        println("Using ${src.size} ${if (src.size > 1) "VCs" else "VC"}:")
        src.forEachIndexed { index, vc -> println("- ${index + 1}. $vc (${vc.readText().toCredential().type.last()})") }

        val vcStrList = src.stream().map { vc -> vc.readText() }.collect(Collectors.toList())

        // Creating the Verifiable Presentation
        val vp = Custodian.getService().createPresentation(vcStrList, holderDid, null, null, null)

        println("\nResults:\n")
        println("Verifiable presentation generated for holder DID: \"$holderDid\"")
        println("Verifiable presentation document (below, JSON):\n\n$vp")

        // Storing VP
        vpFileName = "data/vc/presented/vp-${Timestamp.valueOf(LocalDateTime.now()).time}.json"
        File(vpFileName).writeText(vp)
        println("\nVerifiable presentation was saved to file: \"$vpFileName\"")

        Path(vpFileName).readText() shouldContain "proof"
    }

    "4.4. Verifying the VP" {
        val src = File(vpFileName)
        val policies = listOf("SignaturePolicy", "JsonSchemaPolicy", "TrustedSubjectDidPolicy")


        println("Verifying from file \"$src\"...\n")

        when {
            !src.exists() -> throw Exception("Could not load file: \"$src\".")
            policies.any { !PolicyRegistry.contains(it) } -> throw Exception(
                "Unknown verification policy specified: ${policies.minus(PolicyRegistry.listPolicies()).joinToString()}"
            )
        }

//        val type = when (verificationResult.verificationType) {
//            VerificationType.VERIFIABLE_PRESENTATION -> "verifiable presentation"
//            VerificationType.VERIFIABLE_CREDENTIAL -> "verifiable credential"
//        }

//        println(
//            when (verificationResult.verified) {
//                true -> "The $type was verified successfully."
//                false -> "The $type is not valid or could not be verified."
//            }
//        )

        val verificationResult = Auditor.verify(src.readText(), policies.map { PolicyRegistry.getPolicy(it) })

        println("\nResults:\n")

        verificationResult.policyResults.forEach { (policy, result) ->
            println("$policy:\t\t $result")
            result shouldBe true
        }
        println("Verified:\t\t ${verificationResult.overallStatus}")

        verificationResult.overallStatus shouldBe true
    }
}) {
    override fun testCaseOrder(): TestCaseOrder = TestCaseOrder.Sequential
}
