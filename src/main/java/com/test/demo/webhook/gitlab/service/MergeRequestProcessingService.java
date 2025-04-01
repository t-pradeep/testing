package com.test.demo.webhook.gitlab.service;

import com.test.demo.webhook.gitlab.client.GitLabApiClient;
import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import com.test.demo.webhook.gitlab.processor.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MergeRequestProcessingService {
    private static final String UNKNOWN_VERSION = "unknown";
    private static final String POM_XML_PATH = "pom.xml";

    private final MergeRequestValidator validator;
    private final FileChangeAnalyzer changeAnalyzer;
    private final GitLabApiClient gitLabApiClient;
    private final VersionExtractor versionExtractor;

    public Mono<Void> process(MergeRequestEvent event) {
        if (!validator.validate(event)) {
            return Mono.empty();
        }

        return changeAnalyzer.findChangedApiSpecFiles(event)
            .filter(changes -> !changes.isEmpty())
            .flatMap(changes -> processValidMerge(event, changes.get(0)))
            .then();
    }

    private Mono<Void> processValidMerge(MergeRequestEvent event, String specFilePath) {
        String commitSha = event.attributes().lastCommit().id();
        String shortCommitSha = getShortSha(commitSha);

        Mono<String> pomVersion = fetchAndParsePomVersion(event, commitSha);
        Mono<String> apiSpecVersion = fetchAndParseApiSpecVersion(event, commitSha, specFilePath);

        return Mono.zip(pomVersion, apiSpecVersion)
            .doOnSuccess(versions -> logVersions(
                event.attributes().iid(),
                versions.getT1(),
                versions.getT2(),
                shortCommitSha,
                event.attributes().targetBranch(),
                event.attributes().url()
            ))
            .then();
    }

    private Mono<String> fetchAndParsePomVersion(MergeRequestEvent event, String commitSha) {
        return gitLabApiClient.getFileContent(
                event.attributes().targetProjectId(),
                POM_XML_PATH,
                commitSha
            )
            .flatMap(content -> Mono.fromCallable(() -> 
                versionExtractor.extractPomVersion(content)
            ))
            .onErrorResume(e -> {
                log.error("Failed to get pom version: {}", e.getMessage());
                return Mono.just(UNKNOWN_VERSION);
            });
    }

    private Mono<String> fetchAndParseApiSpecVersion(MergeRequestEvent event, String commitSha, String specFilePath) {
        return gitLabApiClient.getFileContent(
                event.attributes().targetProjectId(),
                specFilePath,
                commitSha
            )
            .flatMap(content -> Mono.fromCallable(() -> 
                versionExtractor.extractApiSpecVersion(content, specFilePath)
            ))
            .onErrorResume(e -> {
                log.error("Failed to get API spec version: {}", e.getMessage());
                return Mono.just(UNKNOWN_VERSION);
            });
    }

    private void logVersions(Long mrId, String pomVersion, String apiSpecVersion,
                           String shortSha, String targetBranch, String mrUrl) {
        String codeVersion = UNKNOWN_VERSION.equals(pomVersion) ? 
            UNKNOWN_VERSION : pomVersion + "-" + shortSha;

        log.info("Extracted Details for MR !{}: CodeVersion='{}', ApiSpecVersion='{}', Commit='{}', TargetBranch='{}', MR_URL='{}'",
            mrId, codeVersion, apiSpecVersion, shortSha, targetBranch, mrUrl);
    }

    private String getShortSha(String commitSha) {
        return commitSha != null && commitSha.length() >= 8 ? 
            commitSha.substring(0, 8) : UNKNOWN_VERSION;
    }
}
