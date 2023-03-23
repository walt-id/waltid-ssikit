package system

import future.keywords.in
import future.keywords.every

default main = false

roles = ["family", "friend"]
grants = {
    "family": ["apply_to_masters", "get_grades"],
    "friend": ["get_grades"]
}
constraints = {
    "get_grades": ["location", "time"],
    "apply_to_masters": ["location"]
}

# all inputs must contain user and actions

main {
    input.parameter.user == input.credentialData.id
    input.credentialData.role in roles
    input.parameter.action in grants[input.credentialData.role]

    input.parameter.action == input.credentialData.grant

    every constraint in constraints[input.parameter.action] {
        input.credentialData.constraints[constraint] == input.parameter[constraint]
    }
}
