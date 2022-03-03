# Functional overview

The Walt.ID SSI Kit is a holistic SSI solution, with primarily focus on the European EBSI/ESSIF ecosystem.

The core services are in the scope of:
- **Key Management** generation, import/export
- **Decentralized Identifier (DID)** operations (register, update, deactivate)
- **Verifiable Credential (VC)** operations (issue, present, verify)
- **ESSIF/EBSI** related Use Cases (onboarding, VC exchange, etc.)

The ESSIF/EBSI functions are in the scope of:
- **Onboarding ESSIF/EBSI** onboarding a natural person/legal entity including the DID creation and registration
- **Enable Trusted Issuer** process for entitling a leagal entity to become a Trusted Issuer in the ESSIF ecosystem.
- **Credential Issuance** protocols and data formats for issuing W3C credentials from an Trusted Issuer to a natural person.
- **Credential Verification** verification facilities in order to determine the validity of a W3C verifiable credential aligned with ESSIF/EBSI standards

The library is written in **Kotlin/Java based library** and can be directly integrated as Maven/Gradle dependency. Alternatively the library or the additional **Docker container** can be run as RESTful webservice.

The **CLI tool** conveniently allows running all included functions manually.
