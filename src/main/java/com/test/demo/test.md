package policies.test.bff_test

import rego.v1
import data.policies.test.bff

# Positive Tests: Cases where allow should be true

test_allow_api_ea_view if {
    bff.allow with input as {
        "approval_type": "api",
        "roles": ["ea"],
        "action": "view"
    }
}

test_allow_api_ea_update if {
    bff.allow with input as {
        "approval_type": "api",
        "roles": ["ea"],
        "action": "update"
    }
}

test_allow_api_sa_view if {
    bff.allow with input as {
        "approval_type": "api",
        "roles": ["sa"],
        "action": "view"
    }
}

test_allow_components_ea_view if {
    bff.allow with input as {
        "approval_type": "components",
        "roles": ["ea"],
        "action": "view"
    }
}

test_allow_components_ea_update if {
    bff.allow with input as {
        "approval_type": "components",
        "roles": ["ea"],
        "action": "update"
    }
}

test_allow_components_sa_view if {
    bff.allow with input as {
        "approval_type": "components",
        "roles": ["sa"],
        "action": "view"
    }
}

# Negative Tests: Cases where allow should be false

test_deny_api_ea_create if {
    not bff.allow with input as {
        "approval_type": "api",
        "roles": ["ea"],
        "action": "create"
    }
}

test_deny_api_sa_update if {
    not bff.allow with input as {
        "approval_type": "api",
        "roles": ["sa"],
        "action": "update"
    }
}

test_deny_components_sa_update if {
    not bff.allow with input as {
        "approval_type": "components",
        "roles": ["sa"],
        "action": "update"
    }
}

test_deny_nonexistent_approval_type if {
    not bff.allow with input as {
        "approval_type": "nonexistent",
        "roles": ["ea"],
        "action": "view"
    }
}

test_deny_no_roles if {
    not bff.allow with input as {
        "approval_type": "api",
        "roles": [],
        "action": "view"
    }
}

test_deny_nonexistent_role if {
    not bff.allow with input as {
        "approval_type": "api",
        "roles": ["nonexistent"],
        "action": "view"
    }
}

test_deny_missing_approval_type if {
    not bff.allow with input as {
        "roles": ["ea"],
        "action": "view"
    }
}

test_deny_missing_roles if {
    not bff.allow with input as {
        "approval_type": "api",
        "action": "view"
    }
}

test_deny_missing_action if {
    not bff.allow with input as {
        "approval_type": "api",
        "roles": ["ea"]
    }
}