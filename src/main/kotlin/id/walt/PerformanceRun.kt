package id.walt

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CIssuer
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.measureTimeMillis

val RESOURCES_PATH: String = "src/test/resources"

fun getTemplate(name: String): VerifiableCredential =
    VerifiableCredential.fromString(File("$RESOURCES_PATH/verifiable-credentials/vc-template-default.json").readText(Charsets.UTF_8))

suspend fun main() {
    ServiceMatrix("service-matrix.properties")
    val issuerWebDid: String = DidService.create(DidMethod.web)

    //val credentialService: JwtCredentialService = JwtCredentialService.getService()
    val signatory = Signatory.getService()
    val template = getTemplate("ebsi-attestation")

    template.issuer = W3CIssuer(issuerWebDid)
    template.credentialSubject!!.id = issuerWebDid // self signed

    println("" + measureTimeMillis {
        val jobs = ArrayList<Job>()
        repeat(500000) {
            jobs.add(GlobalScope.launch {
                signatory.issue("VerifiableId", ProofConfig(issuerDid = issuerWebDid, proofType = ProofType.JWT))
            })
        }

        jobs.forEach { it.join() }
    } + " ms overall")
}
