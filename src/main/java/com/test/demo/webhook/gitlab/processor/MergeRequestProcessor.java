package com.test.demo.webhook.gitlab.processor;

import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import com.test.demo.webhook.gitlab.service.MergeRequestService; // Import new service
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component; // Use @Component for processors
import reactor.core.publisher.Mono;

@Component // Changed from @Service
@Slf4j
@RequiredArgsConstructor
public class MergeRequestProcessor {

    private final MergeRequestValidator validator;
    private final FileChangeAnalyzer changeAnalyzer;
    private final MergeRequestService mergeRequestService; // Inject new service

    /**
     * Processes the incoming merge request event.
     * Validates the event, finds relevant changes, and delegates to the service for further processing.
     * @param event The merge request event.
     * @return A Mono indicating completion.
     */
    public Mono<Void> processEvent(MergeRequestEvent event) {
        if (!validator.validate(event)) {
            log.debug("MR event validation failed for MR !{}", event.attributes() != null ? event.attributes().iid() : "unknown");
            return Mono.empty(); // Event is not valid or not relevant, stop processing.
        }

        // Find changed API spec files and process the first one found.
        return changeAnalyzer.findChangedApiSpecFiles(event)
            .filter(changes -> !changes.isEmpty()) // Proceed only if relevant files changed
            .flatMap(changes -> {
                String specFilePath = changes.get(0); // Process the first relevant change
                // Delegate the core logic to the service
                return mergeRequestService.processMergeRequest(event, specFilePath);
            })
            .doOnError(e -> log.error("Error processing MR !{}: {}", event.attributes().iid(), e.getMessage(), e))
            .onErrorResume(e -> {
                // Ensure completion even if the service fails (error is already logged)
                return Mono.empty(); 
            })
            .then(); // Ensure the final result is Mono<Void>
    }
}
