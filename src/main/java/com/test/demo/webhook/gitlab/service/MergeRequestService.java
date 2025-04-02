package com.test.demo.webhook.gitlab.service;

import com.test.demo.webhook.gitlab.client.GitLabApiClient;
import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
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

    private Mono<String> fetchAndParsePomVersion(MergeRequestEvent event, String commitSha) {
        return fetchFileContent(event.attributes().targetProjectId(), POM_XML_PATH, commitSha)
            .flatMap(content -> Mono.fromCallable(() -> versionExtractor.extractPomVersion(content)))
            .onErrorResume(e -> {
                // Use WARN level for expected issues like parsing failures
                log.warn("Failed to get pom version for commit {}: {}", getShortSha(commitSha), e.getMessage());
                return Mono.just(UNKNOWN_VERSION);
            })
            .defaultIfEmpty(UNKNOWN_VERSION); // Handle case where file content is empty
    }

    private Mono<String> fetchAndParseApiSpecVersion(MergeRequestEvent event, String commitSha, String specFilePath) {
        return fetchFileContent(event.attributes().targetProjectId(), specFilePath, commitSha)
            .flatMap(content -> Mono.fromCallable(() -> versionExtractor.extractApiSpecVersion(content, specFilePath)))
            .onErrorResume(e -> {
                 // Use WARN level for expected issues like parsing failures
                log.warn("Failed to get API spec version for {} in commit {}: {}", specFilePath, getShortSha(commitSha), e.getMessage());
                return Mono.just(UNKNOWN_VERSION);
            })
            .defaultIfEmpty(UNKNOWN_VERSION); // Handle case where file content is empty
    }

    private Mono<String> fetchFileContent(Long projectId, String filePath, String ref) {
        // Delegate actual API call to the client
        return gitLabApiClient.getFileContent(projectId, filePath, ref);
    }

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
