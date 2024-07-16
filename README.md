## MOVED to the new repo here => https://github.com/walt-id/waltid-identity

<br />
<div align="center">
    
 <h1>SSI Kit</h1>
 <span>by </span><a href="https://walt.id">walt.id</a>
 <p>Use digital identity<p>


[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=walt-id_waltid-ssikit&metric=security_rating)](https://sonarcloud.io/dashboard?id=walt-id_waltid-ssikit)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=walt-id_waltid-ssikit&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=walt-id_waltid-ssikit)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=walt-id_waltid-ssikit&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=walt-id_waltid-ssikit)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=walt-id_waltid-ssikit&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=walt-id_waltid-ssikit)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=walt-id_waltid-ssikit&metric=ncloc)](https://sonarcloud.io/dashboard?id=walt-id_waltid-ssikit)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=walt-id_waltid-ssikit-examples&metric=alert_status)](https://sonarcloud.io/dashboard?id=walt-id_waltid-ssikit)

[![CI/CD Workflow for walt.id SSI Kit](https://github.com/walt-id/waltid-ssikit/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/walt-id/waltid-ssikit/actions/workflows/build.yml)
<a href="https://walt.id/community">
<img src="https://img.shields.io/badge/Join-The Community-blue.svg?style=flat" alt="Join community!" />
</a>
<a href="https://twitter.com/intent/follow?screen_name=walt_id">
<img src="https://img.shields.io/twitter/follow/walt_id.svg?label=Follow%20@walt_id" alt="Follow @walt_id" />
</a>


</div>

## Discontinuation Notice

**Important:** Please be informed that, beginning from December 2023, the SSI Kit will no longer receive new features. Furthermore, the SSI Kit is planned for discontinuation by the end of Q3 2024.
<br />
<br />
However, all functionalities offered by the SSI Kit will be integrated into our new libraries, APIs, and apps in the walt.id [identity repo](https://github.com/walt-id/waltid-identity). Giving you more modularity, flexibility and ease-of-use to build end-to-end digital identity and wallet solutions.
<br />
<br />
For any clarification or queries, feel free to [contact us](https://walt.id/discord) as we aim to make this transition as smooth as possible.

<hr />

## Getting Started

- [CLI | Command Line Interface](https://docs.walt.id/v/ssikit/getting-started/cli-command-line-interface) - Try out the functions of the SSI Kit locally.
- [REST Api](https://docs.walt.id/v/ssikit/getting-started/rest-apis) - Use the functions of the SSI Kit via an REST api. 
- [Maven/Gradle Dependency](https://docs.walt.id/v/ssikit/getting-started/dependency-jvm) - Use the functions of the SSI Kit directly in a Kotlin/Java project.
- [Example Projects](https://github.com/walt-id/waltid-ssikit-examples) - Demonstrate how to use the SSI Kit in any Kotlin/Java app

Check out the **[Official Documentation](https://docs.walt.id/v/ssikit)**, to dive deeper into the architecture and configuration options available.


## What is the SSI Kit?

A **library** written in Kotlin/Java **to manage Keys, DIDs and VCs**. Functions can be used via **Maven/Gradle** or a **REST api**.

### Features
- **Key Management** generation, import/export
- **Decentralized Identifier (DID)** operations (create, register, update, deactivate)
- **Verifiable Credential (VC)** operations (issue, present, verify)
- **EBSI/ESSIF** related Use Cases (onboarding, VC exchange, etc.)

#### For EBSI
- **Onboarding EBSI/ESSIF** onboarding a natural person/legal entity including the DID creation and registration
- **Enable Trusted Issuer** process for entitling a legal entity to become a Trusted Issuer in the ESSIF ecosystem.
- **Credential Issuance** protocols and data formats for issuing W3C credentials from a Trusted Issuer to a natural person.
- **Credential Verification** verification facilities in order to determine the validity of a W3C Verifiable Credential aligned with EBSI/ESSIF standards.


## Example

- Creating W3C Decentralized Identifiers 
- Issuing/verifying W3C Verifiable Credentials in JSON_LD and JWT format

```kotlin
fun main() {
    // Load services
    ServiceMatrix("service-matrix.properties")

    // Create DIDs
    val issuerDid = DidService.create(DidMethod.ebsi)
    val holderDid = DidService.create(DidMethod.key)

    // Issue VC with LD_PROOF and JWT format (for show-casing both formats)
    val vcJson = Signatory.getService().issue(
        templateIdOrFilename = "VerifiableId",
        config = ProofConfig(issuerDid = issuerDid, subjectDid = holderDid, proofType = ProofType.LD_PROOF)
    )
    val vcJwt = Signatory.getService().issue(
        templateIdOrFilename = "Europass",
        config = ProofConfig(issuerDid = issuerDid, subjectDid = holderDid, proofType = ProofType.JWT)
    )

    // Present VC in JSON-LD and JWT format (for show-casing both formats)
    val vpJson = Custodian.getService().createPresentation(listOf(vcJson), holderDid)
    val vpJwt = Custodian.getService().createPresentation(listOf(vcJwt), holderDid)

    // Verify VPs, using Signature, JsonSchema and a custom policy
    val resJson = Auditor.getService().verify(vpJson, listOf(SignaturePolicy(), JsonSchemaPolicy()))
    val resJwt = Auditor.getService().verify(vpJwt, listOf(SignaturePolicy(), JsonSchemaPolicy()))

    println("JSON verification result: ${resJson.policyResults}")
    println("JWT verification result:  ${resJwt.policyResults}")
}
```

## Join the community

* Connect and get the latest updates: <a href="https://discord.gg/AW8AgqJthZ">Discord</a> | <a href="https://walt.id/newsletter">Newsletter</a> | <a href="https://www.youtube.com/channel/UCXfOzrv3PIvmur_CmwwmdLA">YouTube</a> | <a href="https://mobile.twitter.com/walt_id" target="_blank">Twitter</a>
* Get help, request features and report bugs: <a href="https://github.com/walt-id/.github/discussions" target="_blank">GitHub Discussions</a>

## Standards & Specifications

- [EBSI Wallet Conformance](https://api-conformance.ebsi.eu/docs/wallet-conformance) 
- [Verifiable Credentials Data Model 1.0](https://www.w3.org/TR/vc-data-model/) 
- [OpenID for Verifiable Credentials](https://openid.net/openid4vc/)
- [Decentralized Identifiers (DIDs) v1.0](https://w3c.github.io/did-core/) 
- [DID Method Rubric](https://w3c.github.io/did-rubric/)
- [did:web Decentralized Identifier Method Specification](https://w3c-ccg.github.io/did-method-web/) 
- [The did:key Method v0.7](https://w3c-ccg.github.io/did-method-key/)

## License

**Licensed under the [Apache License, Version 2.0](https://github.com/walt-id/waltid-ssikit/blob/master/LICENSE).**

## Funded & supported by

<a href="https://essif-lab.eu/" target="_blank"><img src="assets/logos-supporter.png"></a>
