package com.test.demo.webhook.gitlab.processor;

import com.test.demo.webhook.gitlab.client.GitLabApiClient;
import com.test.demo.webhook.gitlab.dto.ApiResponses.*;
import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream; // Add import for Stream

@Slf4j
@Component
@RequiredArgsConstructor
public class FileChangeAnalyzer {
    private final Set<String> apiSpecFiles;
    private final GitLabApiClient gitLabApiClient;

    public Mono<List<String>> findChangedApiSpecFiles(MergeRequestEvent event) {
        if (apiSpecFiles.isEmpty()) {
            log.debug("No API spec files configured for checking");
            return Mono.just(Collections.emptyList());
        }

        return gitLabApiClient.getMergeRequestChanges(
            event.attributes().targetProjectId(), 
            event.attributes().iid()
        )
        .map(this::filterRelevantChanges)
        .defaultIfEmpty(Collections.emptyList());
    }

    private List<String> filterRelevantChanges(MergeRequestChanges changes) {
        if (changes == null || changes.changes() == null) {
            log.warn("No changes found in API response");
            return Collections.emptyList();
        }

        // Use flatMap to handle path selection and null filtering more robustly
        return changes.changes().stream()
            .filter(Objects::nonNull) // Ensure Change object is not null
            .flatMap(change -> {
                String path = change.newPath() != null ? change.newPath() : change.oldPath();
                // If path is non-null and contained in the set, stream it, otherwise stream empty
                return (path != null && apiSpecFiles.contains(path)) ? Stream.of(path) : Stream.empty();
            })
            .distinct()
            .collect(Collectors.toList()); // Revert to Collectors.toList() for better type inference compatibility
    }
}
