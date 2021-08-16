package id.walt.signatory

import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.Europass
import kotlin.reflect.KClass

interface SignatoryDataProvider {
    fun populate(template: VerifiableCredential, subjectDid: String, issuerDid: String): VerifiableCredential
}

class MyEuropassDataProvider : SignatoryDataProvider {
    override fun populate(template: VerifiableCredential, subjectDid: String, issuerDid: String): Europass {
        // TODO populate template and return fully defined VerifiableCredential
        val vc = template as Europass
        vc.credentialSubject!!.id = subjectDid
        vc.issuer = issuerDid
        vc.credentialSubject!!.givenNames = "My new name ${System.currentTimeMillis()}"
        vc.credentialSubject!!.familyName = "Family name"
        return vc
    }
}

class NoSuchDataProviderException(credentialType: KClass<out VerifiableCredential>) :
    Exception("No data provider is registered for ${credentialType.simpleName}")

object DataProviderRegistry {
    val providers = HashMap<KClass<out VerifiableCredential>, SignatoryDataProvider>()

    // TODO register via unique stringId
    fun register(credentialType: KClass<out VerifiableCredential>, provider: SignatoryDataProvider) =
        providers.put(credentialType, provider)

    fun getProvider(credentialType: KClass<out VerifiableCredential>) =
        providers[credentialType] ?: throw NoSuchDataProviderException(credentialType)

    init {
        // Init default providers
        register(Europass::class, MyEuropassDataProvider())
    }
}


fun main() {


    /*val vcTemplate = Europass(listOf(""), listOf(""))

    val provider = DataProviderRegistry.getProvider(vcTemplate::class)

    val vc = provider.populate(vcTemplate) as Europass

    println(Klaxon().toJsonString(vc))*/

    // TODO issue VerifiableCredential
}
