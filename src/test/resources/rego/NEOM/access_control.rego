# Input parameter example:
#{
#    "input": {
#        "credentialData": { /* ... whole credential object ... */ },
#        "parameter": {
#                         "type": "StudentCard",                             # required credential type
#                         "acl": [                                           # list of allowed ids, remove or set false if no acl restriction is required
#                             "0904008084H",
#                             "123456789"
#                         ],
#                         "idProp": "credentialSubject.matriculationNumber", # path to id property, for acl
#                         "beginDateProp": "issuanceDate",                   # path to begin date property, remove or set false if no timerange restriction is required
#                         "endDateProp": "expirationDate"                    # path to end date property, remove or set false if no timerange restriction is required
#                     }
#    }
#}
# supported credentials: e.g.: StudentCard or PregnancyCertificate from src/main/resources/vc-templates/NEOM, and others
# example input parameters: OxagonAccessInput.json, PregnancyAccessInput.json from src/test/resources/rego/NEOM


package system
import future.keywords.if

default main := false

# vc type ok, and no acl or timerange are required
main {
    vc_type_ok
    not has_acl
    not has_timerange
}

# vc type ok, user is in acl, and no timerange is required
main {
    vc_type_ok
    has_acl
    user_in_acl
    not has_timerange
}

# vc type ok, vc is within timerange and not expired, and no acl is required
main {
    vc_type_ok
    not has_acl
    has_timerange
    vc_within_time_range
}

# vc type ok, vc is within timerange and not expired, and user is in acl
main {
	vc_type_ok
    has_acl
    has_timerange
	user_in_acl
	vc_within_time_range
}

resolve(path, obj) := v {
    v := object.get(obj, split(path, "."), null)
}

vc_type_ok if input.parameter.type == input.credentialData.type[i]

has_acl if {
    input.parameter.acl
}

user_in_acl if resolve(input.parameter.idProp, input.credentialData) == input.parameter.acl[i]

has_timerange if {
    input.parameter.beginDateProp
    input.parameter.endDateProp
}

vc_within_time_range if {
    beginDate := time.parse_rfc3339_ns(resolve(input.parameter.beginDateProp, input.credentialData))
    endDate := time.parse_rfc3339_ns(resolve(input.parameter.endDateProp, input.credentialData))
    now := time.now_ns()
    endDate > beginDate
    now > beginDate
    endDate > now
}
