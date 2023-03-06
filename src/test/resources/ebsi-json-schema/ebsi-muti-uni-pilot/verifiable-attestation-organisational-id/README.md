# EBSI Legal Entity Verifiable ID

> Schema of an EBSI Verifiable ID for an organization-legal entity participating in the educational use cases

## Changes

### 2022-11

- Pump to json-schema 2020-12

### 2022-03

- Changed `$schema` to `draft-07`.
- Updated EBSI Attestation schema to `2022-02`.
- Fixed typo and `required` definition.

### 2021-12

Initial schema.

Known issues:

- the schema can't be compiled with Ajv. Error: `data/allOf/1/properties/required must be object,boolean,`
- the definitions contain a typo (`schemeD` instead of `schemeID`)
