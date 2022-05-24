package app.rbac

import future.keywords.in
import future.keywords.every

default allow = false

# all inputs must contain user and actions

test {
    input.user == data.id
}
