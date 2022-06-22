package system

default main = false

request := {"method": "POST", "url": "https://compliance.gaia-x.eu/api/v1/participant/verify/raw",
            "headers": { "Content-Type": "application/json; charset=utf-8"},
            "body": {
           "selfDescriptionCredential": input.verifiableCredential[0],
           "complianceCredential": input.verifiableCredential[1]
           }}


response := http.send(request)

main {
#print(request)
#print(response)
    response.status_code == 200
    response.body.conforms == true
    response.body.content.conforms == true
    response.body.isValidSignature == true
    response.body.shape.conforms == true
}
