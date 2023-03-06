Downloaded from https://ec.europa.eu/digital-building-blocks/code/projects/EBSI/repos/json-schema/browse

# EBSI Verifiable Attestation

> Schema of an EBSI Verifiable Attestation

## Changes

### 2022-11_01

- Improve descriptions on several fields
- modify `credentialSchema` to support single object or array of objects
  - will contain `type schema` and optionally `type extensions schema`
- add `termsOfUse`, to be used with `type extensions`
- evidence `id` is optional
- Removed `credentialStatus` StatusList2021Credential specification, as it is a type extension

### 2022-11

- Pump to json-schema 2020-12

### 2022-08

- Added the following `credentialStatus` attributes: `statusPurpose`, `statusListIndex`, `statusListCredential`.

### 2022-05

- Added `validUntil` property.
- Removed the following `evidence` attributes: `verifier`, `evidenceDocument`, `subjectPresence`, `documentPresence`.

### 2022-02

- Changed `$schema` to `draft-07`.
- Added `issued` property.
- Made the following `evidence` attributes required: `verifier`, `evidenceDocument`, `subjectPresence`, `documentPresence`.

### 2021-11

Initial schema.
