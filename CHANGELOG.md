# Changelog

Notable changes since the last release of the [SSI Kit](https://github.com/walt-id/waltid-ssikit). 

## [Unreleased]

## [1.12.0] - 2022-07-19

-   Roadmap
    -   Dynamic Policies powered by the OpenPolicyAgent <https://github.com/walt-id/waltid-roadmap/issues/42>
-   Features
    -   Added support for did:ebsi V2 by @xmartinez15 <https://github.com/walt-id/waltid-ssikit/pull/139>
    -   Added support for Open Badge V3 credential by <https://github.com/walt-id/waltid-ssikit-vclib/issues/50>
    -   Dynamically register opa enabled policies with the auditor <https://github.com/walt-id/waltid-ssikit/pull/140>
-   Fixes
    -   Fix custodian importkey api <https://github.com/walt-id/waltid-ssikit/pull/131>

## [1.11.0] - 2022-05-31

-   Roadmap Items
    -   Verifiable Mandate & Delegation <https://github.com/walt-id/waltid-roadmap/issues/37>
    -   Integration of Open Policy Agent <https://github.com/walt-id/waltid-roadmap/issues/40>
-   Features
    -   Support rego policy via CLI  <https://github.com/walt-id/waltid-ssikit/pull/134>

## [1.10.0] - 2022-05-10

-   Bumped all dependencies

## [1.9.0] - 2022-04-08

-   Features
    -   added Secp256k1, Ed25519 key import test cases for key command, key service, core api <https://github.com/walt-id/waltid-ssikit/issues/116> 
    -   added key delete command and tests for core api and key service <https://github.com/walt-id/waltid-ssikit/issues/114>
    -   Simple s3 storage implementation for HKV store  <https://github.com/walt-id/waltid-ssikit/commit/e70e79e6c1d48832038038f3fa5974dd2f8231a6>
    -   Presentation Exchange protocol 2.0 <https://github.com/walt-id/waltid-ssikit/pull/127>

## [1.8.0] - 2022-03-22

-   Features
    -   Extended CLI tool with OidcCommands
    -   DID import CLI command from file or resolved DID
    -   Added support for ParticipantCredential
-   Fixes
    -   Fixed did:web resolution issue

## [1.7.0] - 2022-02-17

-   Roadmap Items
    -   Completed EBSI Wallet Conformance Tests <https://github.com/walt-id/waltid-roadmap/issues/24>
-   Features
    -   Introduced OIDC SIOPv2 core functionality <https://github.com/walt-id/waltid-ssikit/pull/108>
    -   Support of "issued" attribute in EBSI data models <https://github.com/walt-id/waltid-ssikit/pull/106>

## [1.6.2] - 2022-02-04

-   Features
    -   Upgraded VC-Lib to 1.14.1

## [1.6.1] - 2022-01-31

-   Features
    -   Upgraded VC-Lib to 1.13.0

## [1.6.0] - 2022-01-31

-   Features
    -   2019 09 json schema validation <https://github.com/walt-id/waltid-ssikit/pull/102>
-   Fixes
    -   Fix/ebsi jwt verifiable presentation <https://github.com/walt-id/waltid-ssikit/pull/101>
    -   DidService move max length substring to FileSystem Store <https://github.com/walt-id/waltid-ssikit/issues/100>

## [1.5.0] - 2022-01-27

-   Roadmap Items
    -   <https://github.com/walt-id/waltid-roadmap/issues/26>
    -   <https://github.com/walt-id/waltid-roadmap/issues/30>
-   Features
    -   Revocation service added to Signatory

## [1.4.0] - 2022-01-03

-   adaptations for changes in VerifiableCredential data model of vclib version 1.7.0
-   refactoring of data providers
-   credential timestamps using UTC by default

## [1.3.0] - 2021-12-27

-   Roadmap Items
    -   Support of RSA keys  <https://github.com/walt-id/waltid-roadmap/issues/26>
-   Features
    -   Replaced log4j with slf4j-simple
    -   Creation of Timestamps via REST API <https://github.com/walt-id/waltid-roadmap/issues/25>

## [1.2.0] - 2021-12-12

-   Roadmap Items
    -   EBSI Timestamping service <https://github.com/walt-id/waltid-roadmap/issues/25>

-   Features
    -   Added generic jsonRpcService for working with the EBSI ledger <https://github.com/walt-id/waltid-ssikit/pull/82>
    -   Generation of RSA keys
    -   Secp256k1 based did:key implementation
    -   RSA based did:key implementation
    -   DID import

## [1.1.1] - 2021-12-03

-   Features
    -   Added GaiaxSelfDecription credential <https://github.com/dNationCloud/waltid-ssikit/pull/1> thx to <https://github.com/dNationCloud> & <https://github.com/matofeder>
    -   Added GaiaxServiceOffering credential
    -   Added VerifiableVaccinationCertificate credential <https://github.com/walt-id/waltid-ssikit/pull/80>
    -   Creation of VerifiablePresentations via Custodian REST API <https://github.com/walt-id/waltid-ssikit/issues/62>
    -   Custodians REST API should also offer the management functionality for DIDs <https://github.com/walt-id/waltid-ssikit/issues/71>
    -   Import cryptographic key from did:key <https://github.com/walt-id/waltid-ssikit/commit/4ed0b4c02ff75aad7032109b71414ff32756422a>

## [1.1.0] - 2021-11-25

-   Features
    -   Parameterize did:web creation <https://github.com/walt-id/waltid-ssikit/issues/51>
    -   Automatic deployment of test-system at <https://[core|signatory|custodian|auditor|essif].ssikit.walt.id>
    -   Introduced TrustedSchemaRegistry Policy for validating Json-schemas against the EBSI TSR
    -   JsonSchemaPolicy now validates against Json-schemas maintained in the VcLib
    -   Did Document context can be a single string or a list of strings <https://github.com/walt-id/waltid-ssikit/pull/60>
    -   Merging Data Provider - Signatory API now takes credential data as well <https://github.com/walt-id/waltid-ssikit/pull/74>

## [1.0.1] - 2021-11-08

-   Features
    -   SIOPv2 data structures  <https://github.com/walt-id/waltid-ssikit/pull/59>

## [1.0.0] - 2021-11-07

-   Roadmap Items
    -   Init Key Management <https://github.com/walt-id/waltid-roadmap/issues/8>
    -   Init Decentralized Identifiers <https://github.com/walt-id/waltid-roadmap/issues/11>
    -   Signatory <https://github.com/walt-id/waltid-roadmap/issues/9>
    -   Custodian <https://github.com/walt-id/waltid-roadmap/issues/11>
    -   Auditor <https://github.com/walt-id/waltid-roadmap/issues/15>
    -   ESSIF | DID -basic <https://github.com/walt-id/waltid-roadmap/issues/2>
    -   ESSIF VC verification - basic  <https://github.com/walt-id/waltid-roadmap/issues/6>

-   Features 
    -   Server Binding-Address must be configurable <https://github.com/walt-id/waltid-ssikit/issues/1>
    -   Loading issuer from EBSI <https://github.com/walt-id/waltid-ssikit/pull/37>
    -   Abstract BaseDid <https://github.com/walt-id/waltid-ssikit/pull/38>
    -   Trusted Issuer Registry Policy <https://github.com/walt-id/waltid-ssikit/pull/39>
    -   Persistence context <https://github.com/walt-id/waltid-ssikit/pull/41>
    -   New EBSI DID format <https://github.com/walt-id/waltid-ssikit/pull/42>
    -   DID Document context <https://github.com/walt-id/waltid-ssikit/issues/44>
    -   Feat SIOP <https://github.com/walt-id/waltid-ssikit/pull/45>
    -   Feat/ebsi vc and vp verifications <https://github.com/walt-id/waltid-ssikit/pull/48>
    -   Update GaiaxCredential <https://github.com/walt-id/waltid-ssikit/pull/50>
    -   Replacement of existing key-alias when importing keys <https://github.com/walt-id/waltid-ssikit/pull/54>

-   Fixes
    -   Swagger Docu is broken (no docs nor parameters are shown)  <https://github.com/walt-id/waltid-ssikit/issues/20>
    -   Key export/import of EdDSA_ED25519 not working <https://github.com/walt-id/waltid-ssikit/issues/18>
    -   Fix/ebsi onboarding ephemeral key service <https://github.com/walt-id/waltid-ssikit/pull/40>
    -   Fixed the way of finding the padding <https://github.com/walt-id/waltid-ssikit/pull/43>
    -   SignaturePolicy fails for issuers using did:ebsi <https://github.com/walt-id/waltid-ssikit/issues/52>

[Unreleased]: https://github.com/walt-id/waltid-ssikit/compare/1.12.0...HEAD

[1.12.0]: https://github.com/walt-id/waltid-ssikit/compare/1.11.0...1.12.0

[1.11.0]: https://github.com/walt-id/waltid-ssikit/compare/1.10.0...1.11.0

[1.10.0]: https://github.com/walt-id/waltid-ssikit/compare/1.9.0...1.10.0

[1.9.0]: https://github.com/walt-id/waltid-ssikit/compare/1.8.0...1.9.0

[1.8.0]: https://github.com/walt-id/waltid-ssikit/compare/1.7.0...1.8.0

[1.7.0]: https://github.com/walt-id/waltid-ssikit/compare/1.6.2...1.7.0

[1.6.2]: https://github.com/walt-id/waltid-ssikit/compare/1.6.1...1.6.2

[1.6.1]: https://github.com/walt-id/waltid-ssikit/compare/1.6.0...1.6.1

[1.6.0]: https://github.com/walt-id/waltid-ssikit/compare/1.5.0...1.6.0

[1.5.0]: https://github.com/walt-id/waltid-ssikit/compare/1.4.0...1.5.0

[1.4.0]: https://github.com/walt-id/waltid-ssikit/compare/1.3.0...1.4.0

[1.3.0]: https://github.com/walt-id/waltid-ssikit/compare/1.2.0...1.3.0

[1.2.0]: https://github.com/walt-id/waltid-ssikit/compare/1.2.0...1.2.0

[1.2.0]: https://github.com/walt-id/waltid-ssikit/compare/1.1.1...1.2.0

[1.1.1]: https://github.com/walt-id/waltid-ssikit/compare/1.1.0...1.1.1

[1.1.0]: https://github.com/walt-id/waltid-ssikit/compare/1.0.1...1.1.0

[1.0.1]: https://github.com/walt-id/waltid-ssikit/compare/1.0.0...1.0.1

[1.0.0]: https://github.com/walt-id/waltid-ssikit/compare/2be9d92014df8b7da68ccccc96bdd1024f2ce50e...1.0.0
