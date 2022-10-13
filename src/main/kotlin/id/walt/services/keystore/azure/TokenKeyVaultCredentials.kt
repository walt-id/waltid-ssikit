package id.walt.services.keystore.azure

import com.microsoft.aad.adal4j.AuthenticationContext
import com.microsoft.aad.adal4j.ClientCredential
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials
import java.util.concurrent.*
import kotlin.math.min

class TokenKeyVaultCredentials(private val id: String, private val secret: String) : KeyVaultCredentials() {

    override fun doAuthenticate(authorization: String, resource: String, scope: String?): String {
        var token = ""
        var nbRetries = 0
        val maxReties = 32
        var delay = 600L // milliseconds
        val maxDelay = 16000L // 16 seconds

        while (token.isEmpty()) {
            try {
                token = getToken(authorization, resource)
            } catch (e: Exception) {
                // Wait to avoid Azure Key Vault throttle -> https://docs.microsoft.com/en-us/azure/key-vault/key-vault-ovw-throttling
                try {
                    TimeUnit.MILLISECONDS.sleep(delay)
                } catch (sleepy: InterruptedException) {
                    println("Error occurred during waiting $sleepy")
                }
                nbRetries++
                delay = min(delay * 2, maxDelay)
                if (nbRetries > maxReties) {
                    return ""
                }
            }
        }
        return token
    }

    private fun getToken(authorization: String, resource: String): String {
        val service = Executors.newFixedThreadPool(1)
        return try {
            AuthenticationContext(authorization, false, service)
                .acquireToken(resource, ClientCredential(id, secret), null)
                .get().accessToken
        } catch (e: Exception) {
            throw RuntimeException("Azure's client generation failed: " + e.message)
        } finally {
            service.shutdown()
        }
    }
}
