package system
import future.keywords.in

default main = false

main {
    data.type[count(data.type)-1] != "LegalPerson"
} else {
    data.type[count(data.type)-1] == "LegalPerson"
    upper(data["credentialSubject"]["gx-participant:legalAddress"]["gx-participant:addressCountryCode"]) in input.allowed_countries
}

# e.g: verify countries in the EU:
# ssikit vc verify -p DynamicPolicy='{ "policy": "./src/test/resources/rego/country.rego", "dataPath": "$" "input": { "allowed_countries": [ "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PO", "PT", "RO", "SE", "SI", "SK" ] } }' legalperson.json
# Create saved dynamic policy:
# ssikit vc policies create -n "EUPolicy" -p "./src/test/resources/rego/country.rego" -d "$" -i '{ "allowed_countries": [ "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PO", "PT", "RO", "SE", "SI", "SK" ] }'
# ssikit vc verify -p EUPolicy legalperson.json
