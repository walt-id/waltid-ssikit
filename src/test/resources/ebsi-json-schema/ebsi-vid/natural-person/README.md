# EBSI Natural Person Verifiable ID

> Schema of an EBSI Verifiable ID for a natural person

## Changes

### 2022-11

- Pump to json-schema 2020-12

### 2022-02

- Changed `$schema` to `draft-07`.
- Updated EBSI Attestation schema to `2022-02`.

### 2021-12

Initial schema.

Known issues:

- `credentialSubject.id` is not defined in the EBSI Natural Person Verifiable ID schema, yet it is required. Some tools may consider this as an error. For example, [Ajv](https://ajv.js.org/strict-mode.html#defined-required-properties) configured in strict mode would throw an error.
