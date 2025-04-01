package com.test.demo.webhook.gitlab.client; // Correct package

import com.test.demo.webhook.gitlab.dto.ApiResponses.FileContent; // Correct DTO import
import com.test.demo.webhook.gitlab.dto.ApiResponses.MergeRequestChanges; // Correct DTO import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitLabApiClientImpl implements GitLabApiClient {

    // Constants for API paths
    private static final String MERGE_REQUEST_CHANGES_URI = "/projects/%d/merge_requests/%d/changes";
    private static final String REPOSITORY_FILES_URI = "/projects/%d/repository/files/%s?ref=%s";

    @Qualifier("gitlabWebClient") // Ensure correct WebClient bean is injected
    private final WebClient webClient;

    @Override
    public Mono<MergeRequestChanges> getMergeRequestChanges(Long projectId, Long mergeRequestIid) { // Correct return type
        String uri = String.format(MERGE_REQUEST_CHANGES_URI, projectId, mergeRequestIid);
        log.debug("Fetching MR changes from URI: {}", uri);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(MergeRequestChanges.class) // Correct DTO type
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("GitLab API error fetching changes for MR !{} in project {}: {} - {}",
                            mergeRequestIid, projectId, e.getStatusCode(), e.getResponseBodyAsString(), e);
                    return Mono.empty(); // Return empty on error
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Generic error fetching changes for MR !{} in project {}: {}",
                            mergeRequestIid, projectId, e.getMessage(), e);
                    return Mono.empty(); // Return empty on error
                })
                .doOnSuccess(changes -> {
                    if (changes == null) {
                        log.warn("Received null changes response for MR !{} in project {}", mergeRequestIid, projectId);
                    } else {
                        log.debug("Successfully fetched {} changes for MR !{} in project {}",
                                changes.changes() != null ? changes.changes().size() : 0, mergeRequestIid, projectId);
                    }
                });
    }

    @Override
    public Mono<String> getFileContent(Long projectId, String filePath, String ref) {
        // File paths in URLs need to be URL-encoded (e.g., '/' becomes '%2F')
        String encodedFilePath = UriUtils.encode(filePath, StandardCharsets.UTF_8);
        String uri = String.format(REPOSITORY_FILES_URI, projectId, encodedFilePath, ref);
        String shortSha = ref != null && ref.length() >= 8 ? ref.substring(0, 8) : ref;

        log.debug("Fetching file content: project={}, path={}, ref={}", projectId, filePath, shortSha);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        clientResponse -> {
                            // Log 404 specifically and return empty - this is often expected
                            log.warn("File not found via GitLab API: project={}, path={}, ref={}", projectId, filePath, shortSha);
                            return Mono.empty(); // Handled by downstream logic (e.g., defaultIfEmpty)
                        })
                .onStatus(HttpStatus::isError, clientResponse ->
                        // Handle other errors by logging and returning an error signal
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    String errorMessage = String.format("GitLab API error %s fetching file: project=%d, path=%s, ref=%s. Body: %s",
                                            clientResponse.statusCode(), projectId, filePath, shortSha, body);
                                    log.error(errorMessage);
                                    return Mono.error(new GitLabApiException(errorMessage, clientResponse.statusCode()));
                                })
                                .switchIfEmpty(Mono.error(new GitLabApiException( // Handle cases where error body is empty
                                        String.format("GitLab API error %s fetching file: project=%d, path=%s, ref=%s. Empty response body.",
                                                clientResponse.statusCode(), projectId, filePath, shortSha),
                                        clientResponse.statusCode())))
                )
                .bodyToMono(GitLabFileContent.class)
                .flatMap(fileContent -> decodeFileContent(fileContent, filePath))
                .onErrorResume(GitLabApiException.class, e -> {
                    // Errors handled by onStatus are caught here, just return empty
                    return Mono.empty();
                })
                 .onErrorResume(Exception.class, e -> {
                    // Catch any other unexpected errors during processing
                    log.error("Unexpected error processing file content response for project={}, path={}, ref={}: {}",
                              projectId, filePath, shortSha, e.getMessage(), e);
                    return Mono.empty();
                 });
    }

    private Mono<String> decodeFileContent(FileContent fileContent, String filePath) { // Correct DTO type
        if (fileContent == null || !"base64".equalsIgnoreCase(fileContent.encoding())) {
            log.error("Invalid content received for file '{}': encoding is not base64 or content is null", filePath);
            // Return an error signal instead of throwing exception directly in flatMap
            return Mono.error(new IllegalArgumentException("Invalid file content encoding or null content from GitLab API for " + filePath));
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(fileContent.content());
            String content = new String(decodedBytes, StandardCharsets.UTF_8);
            log.debug("Successfully decoded content for file '{}'", filePath);
            return Mono.just(content);
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode Base64 content for file '{}': {}", filePath, e.getMessage());
            // Return an error signal
            return Mono.error(new IllegalArgumentException("Failed to decode Base64 content for " + filePath, e));
        }
    }

    // Custom exception class for GitLab API errors
    public static class GitLabApiException extends RuntimeException {
        private final HttpStatus statusCode;

        public GitLabApiException(String message, HttpStatus statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public HttpStatus getStatusCode() {
            return statusCode;
        }
    }
}
