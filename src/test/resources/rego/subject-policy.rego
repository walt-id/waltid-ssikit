package system

default main = false

main {
    input.parameter.user == input.credentialData.credentialSubject.id
}
