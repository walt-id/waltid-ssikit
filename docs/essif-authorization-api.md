# ESSIF Authorization API

Specification https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=2.+Authorization+API


## Questions

# Authentication-Request-Payload

{
    "scope": "openid did_authn",
    "iss": "did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e",  ===> same as jwks_uri => https-lookup ok? auth required?
    "response_type": "id_token",
    "client_id": "<redirect-uri>", ===> needs to be validated? is a redirect necessary?
    "nonce": "<random-nonce>",
    "registration": {
    "redirect_uris": ["https://app.ebsi.xyz"], ===> needs to be validated? is a redirect necessary?
    "response_types": "id_token",
    "id_token_signed_response_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
    "request_object_signing_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
    "access_token_signing_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
    "access_token_encryption_alg_values_supported": ["ECDH-ES"],
    "access_token_encryption_enc_values_supported": ["A128GCM", "A256GCM"],
    "jwks_uri": "https://api.ebsi.xyz/did/1.0/identifiers/did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e"
},

"claims": {
    "id_token": {
    "verified_claims": {
    "verification": {
    "trust_framework": "EBSI", ===> what values are allowed?
    "evidence": {
    "type": {
    "value": "verifiable_credential" ===> what values are allowed?
},
    "document": { ===> credential query by type + schema, right?
    "type": {
    "essential": true,
    "value": ["VerifiableCredential", "VerifiableAuthorisation"]
},
"credentialSchema": {
    "id": {
    "essential": true,
    "value": "https://ebsi.xyz/trusted-schemas-registry/verifiable-authorisation"
}
}


# Authentication Response

{
    "iss": "did:ebsi:0x123abc",
    "sub": "{thumbprint of the sub_jwk}",
    "aud": "did:ebsi:RP-did-here",
    "iat": 1610714000,
    "exp": 1610714900,
    "sub_jwk":{signing JWK}, ===> Public JWK key?
    "sub_did_verification_method_uri": "did:ebsi:0x123abc#authentication-key-proof-3",
    "nonce": "n-0S6_WzA2M", ===> same as above (authRequest.nonce), or freshly generated?
    "claims": {
    "verified_claims": {Authentication-Response-Verifiable-Presentation},
    "encryption_key": {JWK encryption key} ===> Where is this coming from?
    }
}

# Authentication-Response-Verifiable-Presentation
- Credential Schema? Should probably match: https://ebsi.xyz/trusted-schemas-registry/verifiable-authorisation

{
"@context": [
"https://www.w3.org/2018/credentials/v1"
],
"type": [
"VerifiablePresentation"
],
"verifiableCredential": [{Verifiable Authorisation}], ===> does not have to be a list
"holder": "{client DID}", ===> should be a string value
"proof": {
"type": "Ed25519Signature2018",
"created": "2020-04-26T21:24:11Z",
"domain": "api.ebsi.xyz",
"jws": "eyJhbGciOiJFZERTQSIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..53JU-HrqnrTM46mcopmeSQ016lbqWh7rXH-pGvPuZ9GwxqkGz2Qn4eE5ySLFmvbLC59nBx55rrydxLF1JIJuCw",
"proofPurpose": "authentication",
"verificationMethod": "{client DID}#authentication-key-2"
}
}


/siop-session response example, is probably wrong

{
"grantType": "client_credentials",
"clientAssertionType": "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
"clientAssertion": "eyJhbGciOiJIUzI...",
"scope": "openid did_authn"
}

#Access Token and Authenticated Key Exchange - AKE1 Protocol https://toc.cryptobook.us/book.pdf

https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=Authorisation+API
https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/DID+Authentication+Library


{
"ake1_enc_payload": "{encrypted JWS encoded access token}",
"ake1_jws_detached": "{detached JWS of ake1_sig_payload}",===> what does detached JWS mean?
"did": "{did of the RP}" ===> which DID?
}
{
"ake1_nonce": "{Nonce from the ID Token used for authentication}",
"ake1_enc_payload": "{ake1_enc_payload}",
"did": "{DID of the Client}"
}




generateEd25519KeyPairNimbus: {"kty":"OKP","d":"ZSUCda_qZxTLvVeVqryEV-s9t1-9ENPo952TM0ZkWp0","use":"sig","crv":"Ed25519","kid":"WaltId-Key-6b0a1ca543c849559dd9abbb9714422d","x":"THmMb65vySw4gso8BU7JLZgpBPV4kxAyHqmqo7hORMA","alg":"EdDSA"}
generateEd25519KeyPairNimbus: {"kty":"OKP","d":"1T8yaqYMzZM-2cA_XZ3UuaeHJV8ZByW574drsjH0ggE","use":"sig","crv":"Ed25519","kid":"WaltId-Key-4bf9c98703b2446987479de12f745485","x":"Yrwu-UrTqdixgSEAJKUZunkBJk9VMNuj8aimCV1VVJE","alg":"EdDSA"}

access_token_response:
{
"ake1_enc_payload": "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6ImRzT0QzTGlnSHZ4UWg0M0hCQ0k4WDlfRjRQd3piTHFUdDJVWjhSbVkweTAifSwiY3R5IjoiSldUIiwiZW5jIjoiQTI1NkdDTSIsImFsZyI6IkVDREgtRVMifQ..xAkpa8Cz5CwyqNfH.GVNo-z8Qd_ZkOQNFyU3CSI33r_FJyheJNjTand3jI5AcKdyVZSsS0iR-FWXB2zl8pScJCCT-b8WWxi77OIIRF7l6LRDm4kWTbhyphFdifvyODBJkWJ7IewMmsZe3I11_geVdjaBBWeW4RkmqOGbEDJSxnBy2574XMnxKj5pCrYCcIRVyNLL2JVYKlC8P7ejWEjZzJQ3QcJZK6sq3oe7xpoZFUXv2XDAN8YGorVAvPIJCeJvkhuxqCSb8GWOheHZFLDgPTttcoOAk2b2f9jx9KcNWOLiEQUj-7gQxHbrcqjaL-DYIV4gyp8WQF7JOihvlCon12PrbGhNpmChS6sOWS83GSdKYObQyLLY5yeBNrvNKT3P2VJPsugZrb5jXgauuG00SO9T4EmSqRfQyKTPOOjlYB6dTIlNRsL0AXHjyZhF5oa79KAPzQ98lV242lb5Xshh71zXJ8YsJr9UC5_CpPL616dYEE-CkM18pCZJNep-ZhlVQnHXe12CjCu3DlwE3MYW3-avLbpAauGJD5RMnrpKdtNY1F4_MHuEXFJm1nAwUfEBdX6wwe8UmSCIEhaq_VrNbJntRD5XwW3ZcZOniP2XR8VrAjs83atvnG4RTnM8JrX4TXIUMqanzI31ePWBK12Va_6PtCaFmFWeCl_BMroSyrRzVa2sN-YxpjARN-GFB1BJrdog5Iydie2ic9Ij9h8zSULTjqR0.iHQTQOKOmKujELsQYEZidg",
"ake1_jws_detached": "eyJraWQiOiJkaWQ6a2V5Ono2TWtqYm5YY21Da3pOU3A0bko2VGU4YWZqek44WWsxZGhrQWp0QUJ3TUc4NFJZSyIsImFsZyI6IkVkRFNBIn0.eyJha2UxX25vbmNlIjoiODQyZDM3NWEtOWEzYy00MTUyLWJjNWYtMDQxY2YyZWIxNjM3IiwiZGlkIjoiZGlkOmtleTp6Nk1rbTZnUnVtOWhoZDc2eG5GcVBtV1RrdDIxWFd4cWNxZEhQV1pna3JxNVlCRmUiLCJha2UxX2VuY19wYXlsb2FkIjoiZXlKbGNHc2lPbnNpYTNSNUlqb2lUMHRRSWl3aVkzSjJJam9pV0RJMU5URTVJaXdpZUNJNkltUnpUMFF6VEdsblNIWjRVV2cwTTBoQ1EwazRXRGxmUmpSUWQzcGlUSEZVZERKVldqaFNiVmt3ZVRBaWZTd2lZM1I1SWpvaVNsZFVJaXdpWlc1aklqb2lRVEkxTmtkRFRTSXNJbUZzWnlJNklrVkRSRWd0UlZNaWZRLi54QWtwYThDejVDd3lxTmZILkdWTm8tejhRZF9aa09RTkZ5VTNDU0kzM3JfRkp5aGVKTmpUYW5kM2pJNUFjS2R5VlpTc1MwaVItRldYQjJ6bDhwU2NKQ0NULWI4V1d4aTc3T0lJUkY3bDZMUkRtNGtXVGJoeXBoRmRpZnZ5T0RCSmtXSjdJZXdNbXNaZTNJMTFfZ2VWZGphQkJXZVc0UmttcU9HYkVESlN4bkJ5MjU3NFhNbnhLajVwQ3JZQ2NJUlZ5TkxMMkpWWUtsQzhQN2VqV0VqWnpKUTNRY0paSzZzcTNvZTd4cG9aRlVYdjJYREFOOFlHb3JWQXZQSUpDZUp2a2h1eHFDU2I4R1dPaGVIWkZMRGdQVHR0Y29PQWsyYjJmOWp4OUtjTldPTGlFUVVqLTdnUXhIYnJjcWphTC1EWUlWNGd5cDhXUUY3Sk9paHZsQ29uMTJQcmJHaE5wbUNoUzZzT1dTODNHU2RLWU9iUXlMTFk1eWVCTnJ2TktUM1AyVkpQc3VnWnJiNWpYZ2F1dUcwMFNPOVQ0RW1TcVJmUXlLVFBPT2psWUI2ZFRJbE5Sc0wwQVhIanlaaEY1b2E3OUtBUHpROThsVjI0MmxiNVhzaGg3MXpYSjhZc0pyOVVDNV9DcFBMNjE2ZFlFRS1Da00xOHBDWkpOZXAtWmhsVlFuSFhlMTJDakN1M0Rsd0UzTVlXMy1hdkxicEFhdUdKRDVSTW5ycEtkdE5ZMUY0X01IdUVYRkptMW5Bd1VmRUJkWDZ3d2U4VW1TQ0lFaGFxX1ZyTmJKbnRSRDVYd1czWmNaT25pUDJYUjhWckFqczgzYXR2bkc0UlRuTThKclg0VFhJVU1xYW56STMxZVBXQksxMlZhXzZQdENhRm1GV2VDbF9CTXJvU3lyUnpWYTJzTi1ZeHBqQVJOLUdGQjFCSnJkb2c1SXlkaWUyaWM5SWo5aDh6U1VMVGpxUjAuaUhRVFFPS09tS3VqRUxzUVlFWmlkZyJ9.2RyFzt5KpuzV2V2EiU-b3WGGZ3Mli616I-MDOuxmBL393ni2hidkgR-YKTq6YxALbaG-SAu-WqmeB1mh4QaTDg",
"did": "did:key:z6MkjbnXcmCkzNSp4nJ6Te8afjzN8Yk1dhkAjtABwMG84RYK"
}

ake1_enc_payload:
{
"access_token": "eyJraWQiOiJMZXRzVHJ1c3QtS2V5LTFhZjNlMWIzNGIxNjQ3NzViNGQwY2IwMDJkODRlZmQwIiwidHlwIjoiSldUIiwiYWxnIjoiRVM1MTIifQ.eyJzdWIiOiIwNTQyNTVhOC1iODJmLTRkZWQtYmQ0OC05NWY5MGY0NmM1M2UiLCJpc3MiOiJodHRwczovL2FwaS5sZXRzdHJ1c3QuaW8iLCJleHAiOjE2MTUyMDg5MTYsImlhdCI6MTYxNTIwNTkxNn0.ARUKAO0f6vpRyUXWWEeL4xPegzl66eaC-AeEXswhsrs1OREae81JPNqnWs8e3rTrRCLCfRTcVS658hV8jfjAAY6vASwtNjV9HwJcmUGmpanBjAuQkJLkmv6Sn3lqzF5PU3hFv3GnVznvcDDyLRlsI8OooPZmM6p-FWUR8tAYKpvzAdMB",
"did": "did:key:z6MkjbnXcmCkzNSp4nJ6Te8afjzN8Yk1dhkAjtABwMG84RYK"
}

ake1_jws_detached:
{
"ake1_nonce": "842d375a-9a3c-4152-bc5f-041cf2eb1637",
"ake1_enc_payload": "eyJlcGsiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6ImRzT0QzTGlnSHZ4UWg0M0hCQ0k4WDlfRjRQd3piTHFUdDJVWjhSbVkweTAifSwiY3R5IjoiSldUIiwiZW5jIjoiQTI1NkdDTSIsImFsZyI6IkVDREgtRVMifQ..xAkpa8Cz5CwyqNfH.GVNo-z8Qd_ZkOQNFyU3CSI33r_FJyheJNjTand3jI5AcKdyVZSsS0iR-FWXB2zl8pScJCCT-b8WWxi77OIIRF7l6LRDm4kWTbhyphFdifvyODBJkWJ7IewMmsZe3I11_geVdjaBBWeW4RkmqOGbEDJSxnBy2574XMnxKj5pCrYCcIRVyNLL2JVYKlC8P7ejWEjZzJQ3QcJZK6sq3oe7xpoZFUXv2XDAN8YGorVAvPIJCeJvkhuxqCSb8GWOheHZFLDgPTttcoOAk2b2f9jx9KcNWOLiEQUj-7gQxHbrcqjaL-DYIV4gyp8WQF7JOihvlCon12PrbGhNpmChS6sOWS83GSdKYObQyLLY5yeBNrvNKT3P2VJPsugZrb5jXgauuG00SO9T4EmSqRfQyKTPOOjlYB6dTIlNRsL0AXHjyZhF5oa79KAPzQ98lV242lb5Xshh71zXJ8YsJr9UC5_CpPL616dYEE-CkM18pCZJNep-ZhlVQnHXe12CjCu3DlwE3MYW3-avLbpAauGJD5RMnrpKdtNY1F4_MHuEXFJm1nAwUfEBdX6wwe8UmSCIEhaq_VrNbJntRD5XwW3ZcZOniP2XR8VrAjs83atvnG4RTnM8JrX4TXIUMqanzI31ePWBK12Va_6PtCaFmFWeCl_BMroSyrRzVa2sN-YxpjARN-GFB1BJrdog5Iydie2ic9Ij9h8zSULTjqR0.iHQTQOKOmKujELsQYEZidg",
"did": "did:key:z6Mkm6gRum9hhd76xnFqPmWTkt21XWxqcqdHPWZgkrq5YBFe"
}


Problems

Nimbus Encrypter does not support secp256k1 keys. Is this a limitation of the library, or is this this generally not possible?
Elliptic Curve Diffie-Hellman encrypter of JWE objects for curves using EC JWK keys. Expects a public EC key (with a P-256, P-384 or P-521 curve).

com.nimbusds.jose.JOSEException: Unsupported elliptic curve secp256k1, must be P-256, P-384 or P-521
