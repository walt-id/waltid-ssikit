# REST APIs


This sections outlines the APIs of the SSI Kit: The _Core API_ , the _ESSIF Connector API_, the _Signatory API_, the _Custodian API_ as well as the _Auditor API_.

In order to review the Swagger doc of each API, just run the **serve** command of the CLI tool and navigate to the corresponding web pages:

        ./ssikit.sh serve

or with Docker:

        docker run -itv $(pwd)/data:/app/data -p 7000-7004:7000-7004 waltid/ssikit -v serve -b 0.0.0.0

- walt.id Core API:      http://127.0.0.1:7000
- walt.id Signatory API: http://127.0.0.1:7001
- walt.id Custodian API: http://127.0.0.1:7002
- walt.id Auditor API:   http://127.0.0.1:7003
- walt.id ESSIF API:     http://127.0.0.1:7004

### Core API

The _Core API_ exposes wallet core functionality in the scope of storing and managing cryptographic keys, Decentralised Identifiers (DIDs) and Verifiable Credentials (VCs).

### Signatory API

The _Signatory API_ exposes the "issuance" endpoint, which provides flexible integration possibilities for anyone intending to issue W3C Verifiable Credentials.

### Custodian API

The _Custodian API_ provides management functions for maintaining sensitive data in a secure way.

### Auditor API

The _Auditor API_ enables anybody to verify W3C Verifiable Credentials. The validation steps can be easily configured by existing or custom _Policies_.

### ESSIF API

The _ESSIF API_ exposes the necessary endpoints for running the ESSIF specific flows between Holders, (Trusted) Issuers (incl. ESSIF Onboarding Service, EOS), Verifier / Relying Party. Aligned with the ESSIF terminology, the API is grouped by the User Wallet (wallet API for consumers / natural persons) and Enterprise Wallet (wallet API for organisations / legal entities).

Note that over the next couple of months the specifications from EBSI and ESSIF are subject of change due to ongoing reviews, improvements and step-wise releases of new versions. This will reflect in continuous updates of the following proposed APIs.

[comment]: <> (TODO __OpenAPI spec: [Wallet API]&#40;ssikit-org-letstrust_wallet_api-0.1-SNAPSHOT-swagger.yaml&#41;__)

[comment]: <> (TODO __OpenAPI spec: [ESSIF Connector API]&#40;ssikit-org-letstrust_essif_connector-0.1-SNAPSHOT-swagger.yaml&#41;__)
