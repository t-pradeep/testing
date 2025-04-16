package policies.test.bff

import rego.v1

# Test that component 'api' with role 'ea' returns sorted actions ["update", "view"]
test_allow_api_ea in {
    input_data := {
        "components": ["api"],
        "role": ["ea"]
    }
    expected := { "api": ["update", "view"] }
    result := { comp: actions | allow[comp] == actions }
    result == expected
}

# Test that component 'api' with role 'sa' returns sorted actions ["view"]
test_allow_api_sa in {
    input_data := {
        "components": ["api"],
        "role": ["sa"]
    }
    expected := { "api": ["view"] }
    result := { comp: actions | allow[comp] == actions }
    result == expected
}

# Test that component 'api' with roles 'ea' and 'sa' returns unique sorted actions ["update", "view"]
test_allow_api_multiple_roles in {
    input_data := {
        "components": ["api"],
        "role": ["ea", "sa"]
    }
    expected := { "api": ["update", "view"] }
    result := { comp: actions | allow[comp] == actions }
    result == expected
}

# Test that non-existent component returns no results
test_allow_non_existent_component in {
    input_data := {
        "components": ["unknown"],
        "role": ["ea"]
    }
    expected := {}
    result := { comp: actions | allow[comp] == actions }
    result == expected
}

# Test that component with role having no actions returns no results
test_allow_no_actions_role in {
    input_data := {
        "components": ["api"],
        "role": ["unknown"]
    }
    expected := {}
    result := { comp: actions | allow[comp] == actions }
    result == expected
}