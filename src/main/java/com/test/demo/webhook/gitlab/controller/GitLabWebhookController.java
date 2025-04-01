package com.test.demo.webhook.gitlab.controller; // Correct package

import com.test.demo.webhook.gitlab.dto.MergeRequestEvent; // Correct DTO import
import com.test.demo.webhook.gitlab.service.MergeRequestProcessingService; // Correct Service import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/webhooks/gitlab")
@RequiredArgsConstructor // Lombok annotation for constructor injection
@Slf4j
public class GitLabWebhookController {

    private final MergeRequestProcessingService mergeRequestProcessingService; // Correct Service type

    @PostMapping("/mergerequest")
    @ResponseStatus(HttpStatus.ACCEPTED) // Acknowledge receipt immediately
    public Mono<Void> handleMergeRequestEvent(@RequestBody MergeRequestEvent event) { // Correct DTO type
        log.info("Received webhook event for MR !{}", event.attributes() != null ? event.attributes().iid() : "unknown");
        // Delegate processing to the service.
        return mergeRequestProcessingService.process(event); // Correct method call
    }
}
