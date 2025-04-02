package com.test.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ConfigurationProperties(prefix = "gitlab.webhook")
@Data
@Validated
public class WebhookProperties {

    @NotEmpty(message = "Target branches must be configured")
    private String targetBranches; // Comma-separated string

    @NotEmpty(message = "API spec files must be configured")
    private List<String> apiSpecFiles;

    /**
     * Returns the target branches as a Set of strings.
     * @return Set of target branch names.
     */
    public Set<String> getTargetBranchesSet() {
        if (targetBranches == null || targetBranches.isBlank()) {
            return Set.of();
        }
        // Split the comma-separated string and trim whitespace
        return Stream.of(targetBranches.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toSet());
    }

     /**
     * Returns the API spec files as a Set of strings.
     * Ensures uniqueness and handles potential null list.
     * @return Set of API spec file paths.
     */
    public Set<String> getApiSpecFilesSet() {
        if (apiSpecFiles == null) {
            return Set.of();
        }
        return Set.copyOf(apiSpecFiles); // Creates an unmodifiable set
    }
}
