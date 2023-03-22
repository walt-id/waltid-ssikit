package system
import future.keywords.if

default main := false

main {
	vc_type_ok
	user_in_acl
}

resolve(path, obj) := v {
    v := object.get(obj, split(path, "."), null)
}

vc_type_ok if input.parameter.type == input.credentialData.type[i]

user_in_acl if resolve(input.parameter.idProp, input.credentialData) == input.parameter.acl[i]
