# ESBI Education Verifiable Accreditation records

> ESBI Education Verifiable Accreditation record schema for educational contexts

## Changes

### 2022-11

- Pump to json-schema 2020-12

### 2022-03

- Changed `$schema` to `draft-07`.
- Updated EBSI Attestation schema to `2022-02`.
- Fixed `authorizationClaims` required properties definition

### 2022-02

**DEPRECATED**.

Unfortunately, this version was containing the wrong data model.

It must not be used anymore. Use `2022-03` instead.

### 2021-12

Initial schema.

Known issues:

- `$schema` points to http://json-schema.org/draft/2020-12/schema instead of https://json-schema.org/draft/2020-12/schema
- the schema can't be compiled with Ajv. Error: `data/allOf/1/properties/credentialSubject/properties/authorizationClaims/properties/required must be object,boolean`
