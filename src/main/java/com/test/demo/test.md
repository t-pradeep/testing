package policies.test.bff_test

import data.policies.test.bff

test_api_ea {
  result := bff.allow with input as { "components": ["api"], "role": ["ea"] }
  result == { "api": ["update", "view"] }
}

test_api_sa {
  result := bff.allow with input as { "components": ["api"], "role": ["sa"] }
  result == { "api": ["view"] }
}

test_api_ea_sa {
  result := bff.allow with input as { "components": ["api"], "role": ["ea", "sa"] }
  result == { "api": ["update", "view"] }
}

test_components_ea {
  result := bff.allow with input as { "components": ["components"], "role": ["ea"] }
  result == { "components": ["update", "view"] }
}

test_nonexistent_component {
  result := bff.allow with input as { "components": ["nonexistent"], "role": ["ea"] }
  result == { "nonexistent": [] }
}

test_no_components {
  result := bff.allow with input as { "components": [], "role": ["ea"] }
  result == {}
}

test_no_roles {
  result := bff.allow with input as { "components": ["api"], "role": [] }
  result == { "api": [] }
}

test_multiple_components {
  result := bff.allow with input as { "components": ["api", "components"], "role": ["ea", "sa"] }
  expected := {
    "api": ["update", "view"],
    "components": ["update", "view"]
  }
  result == expected
}