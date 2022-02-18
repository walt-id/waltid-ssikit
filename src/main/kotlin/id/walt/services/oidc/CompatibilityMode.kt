package id.walt.services.oidc

enum class CompatibilityMode {
  OIDC, // standard oicd response bodies in form-data format
  EBSI_WCT // EBSI_WCT compatible response bodies in json format
}
