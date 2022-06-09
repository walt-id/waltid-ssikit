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
    input.user == data.id
    data.role in roles
    input.action in grants[data.role]

    input.action == data.grant

    every constraint in constraints[input.action] {
        data.constraints[constraint] == input[constraint]
    }
}
