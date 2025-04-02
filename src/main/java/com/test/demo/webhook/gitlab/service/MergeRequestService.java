package com.test.demo.webhook.gitlab.service;

import com.test.demo.webhook.gitlab.client.GitLabApiClient;
import com.test.demo.webhook.gitlab.client.GitLabApiClientImpl; // Import the implementation class
import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import com.test.demo.webhook.gitlab.processor.VersionExtractionException;
import com.test.demo.webhook.gitlab.processor.VersionExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class MergeRequestService {

    private static final String UNKNOWN_VERSION = "unknown";
    private static final String POM_XML_PATH = "pom.xml";

    private final GitLabApiClient gitLabApiClient;
    private final VersionExtractor versionExtractor;

    // Helper function type for version extraction logic
    @FunctionalInterface
    private interface VersionExtractionFunction {
        String extract(String content) throws VersionExtractionException;
    }

    /**
     * Processes a validated merge request event by extracting and logging versions.
     * @param event The merge request event.
     * @param specFilePath The path to the changed API specification file.
     * @return A Mono indicating completion.
     */
    public Mono<Void> processMergeRequest(MergeRequestEvent event, String specFilePath) {
        String commitSha = event.attributes().lastCommit().id();
        String shortCommitSha = getShortSha(commitSha);

        Mono<String> pomVersionMono = fetchAndParsePomVersion(event, commitSha);
        Mono<String> apiSpecVersionMono = fetchAndParseApiSpecVersion(event, commitSha, specFilePath);

        return Mono.zip(pomVersionMono, apiSpecVersionMono)
            .doOnSuccess(versions -> logExtractedVersions(
                event.attributes().iid(),
                versions.getT1(), // pomVersion
                versions.getT2(), // apiSpecVersion
                shortCommitSha,
                event.attributes().targetBranch(),
                event.attributes().url()
            ))
            .then(); // Convert Mono<Tuple2<String, String>> to Mono<Void>
    }

    /**
     * Generic helper to fetch file content and extract a version using a provided function.
     */
    private Mono<String> fetchAndExtractVersion(Long projectId, String filePath, String commitSha,
                                                VersionExtractionFunction extractionFunction, String errorContext) {
        return gitLabApiClient.getFileContent(projectId, filePath, commitSha)
            .flatMap(content -> {
                try {
                    // Use Mono.justOrEmpty to handle null/empty results from extractor gracefully
                    return Mono.justOrEmpty(extractionFunction.extract(content));
                } catch (VersionExtractionException e) {
                    // Log extraction errors and return Mono.error to be caught by onErrorResume
                    log.warn("Failed to extract version from {} for commit {}: {}", filePath, getShortSha(commitSha), e.getMessage());
                    return Mono.error(e); // Propagate specific error
                }
            })
            .onErrorResume(e -> {
                // Catches both API client errors (propagated as GitLabApiException)
                // and VersionExtractionException from flatMap above.
                // Log appropriately but return UNKNOWN_VERSION for processing flow.
                if (!(e instanceof GitLabApiClientImpl.GitLabApiException)) { // Use the implementation class here
                     log.warn("Failed to get {} for commit {}: {}", errorContext, getShortSha(commitSha), e.getMessage());
                }
                // Also log GitLabApiExceptions if needed, but they are already logged in the client
                // else { log.warn("GitLab API error prevented getting {}: {}", errorContext, e.getMessage()); }
                return Mono.just(UNKNOWN_VERSION);
            })
            .defaultIfEmpty(UNKNOWN_VERSION); // Handle case where file content is empty or version is null
    }


    private Mono<String> fetchAndParsePomVersion(MergeRequestEvent event, String commitSha) {
        return fetchAndExtractVersion(
            event.attributes().targetProjectId(),
            POM_XML_PATH,
            commitSha,
            versionExtractor::extractPomVersion, // Pass method reference
            "pom version"
        );
    }

    private Mono<String> fetchAndParseApiSpecVersion(MergeRequestEvent event, String commitSha, String specFilePath) {
         return fetchAndExtractVersion(
            event.attributes().targetProjectId(),
            specFilePath,
            commitSha,
            content -> versionExtractor.extractApiSpecVersion(content, specFilePath), // Pass lambda
            "API spec version from " + specFilePath
        );
    }

    // Removed redundant fetchFileContent method

    private void logExtractedVersions(Long mrId, String pomVersion, String apiSpecVersion,
                                      String shortSha, String targetBranch, String mrUrl) {
        String codeVersion = UNKNOWN_VERSION.equals(pomVersion) ?
            UNKNOWN_VERSION : pomVersion + "-" + shortSha;

        // Keep this log as it seems essential for the application's purpose
        log.info("Extracted Details for MR !{}: CodeVersion='{}', ApiSpecVersion='{}', Commit='{}', TargetBranch='{}', MR_URL='{}'",
            mrId, codeVersion, apiSpecVersion, shortSha, targetBranch, mrUrl);
    }

    private String getShortSha(String commitSha) {
        // Concise way to get short SHA or unknown
        return (commitSha != null && commitSha.length() >= 8) ?
            commitSha.substring(0, 8) : UNKNOWN_VERSION;
    }
}
