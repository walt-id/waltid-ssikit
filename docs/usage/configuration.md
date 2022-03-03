# Configuration

Services come with their own configuration files. For the configuration of service -> implementation mappings, [ServiceMatrix](https://github.com/walt-id/service-matrix) is used.

The default mapping file is "service-matrix.properties", and looks like this:

```properties
id.walt.services.vc.VCService=id.walt.services.vc.LetstrustVCService
id.walt.services.crypto.CryptoService=id.walt.services.crypto.SunCryptoService
id.walt.services.keystore.KeyStoreService=id.walt.services.keystore.SqlKeyStoreService
id.walt.services.key.KeyService=id.walt.services.key.WaltIdKeyService
```

e.g., to change the keystore service, simply replace the line `id.walt.services.keystore.KeyStoreService=id.walt.services.keystore.SqlKeyStoreService` with your own implementation mapping, e.g. for the Azure HSM keystore: `id.walt.services.keystore.KeyStoreService=id.walt.services.keystore.azurehsm.AzureHSMKeystoreService`

To add a service configuration: `id.walt.services.keystore.KeyStoreService=id.walt.services.keystore.SqlKeyStoreService:sql.conf`
Service configuration is by default in HOCON format.
Refer to the specific service on how their configuration is laid out.
