package com.test.demo.webhook.gitlab.controller;

import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import com.test.demo.webhook.gitlab.processor.MergeRequestProcessor; // Import the new processor
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/webhooks/gitlab")
@RequiredArgsConstructor
@Slf4j
public class GitLabWebhookController {

    private final MergeRequestProcessor mergeRequestProcessor; // Inject the processor

    @PostMapping("/mergerequest")
    @ResponseStatus(HttpStatus.ACCEPTED) // Acknowledge receipt immediately
    public Mono<Void> handleMergeRequestEvent(@RequestBody MergeRequestEvent event) {
        log.info("Received webhook event for MR !{}", event.attributes() != null ? event.attributes().iid() : "unknown");
        // Delegate processing to the processor.
        return mergeRequestProcessor.processEvent(event); // Call the processor method
    }
}
