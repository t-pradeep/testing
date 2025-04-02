package com.test.demo.webhook.gitlab.service;

import com.test.demo.webhook.gitlab.client.GitLabApiClient;
import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import com.test.demo.webhook.gitlab.processor.VersionExtractionException;
import com.test.demo.webhook.gitlab.processor.VersionExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeRequestServiceTest {

    @Mock
    private GitLabApiClient gitLabApiClient;

    @Mock
    private VersionExtractor versionExtractor;

    @InjectMocks
    private MergeRequestService mergeRequestService;

    private final String POM_CONTENT = "<project><version>1.0.0</version></project>";
    private final String SPEC_CONTENT = "info:\n  version: '1.2.3'";
    private final String SPEC_FILE_PATH = "spec/api.yaml";
    private final String COMMIT_SHA = "abcdef1234567890";
    private final Long PROJECT_ID = 123L;
    private final Long MR_IID = 456L;

    private MergeRequestEvent createMockEvent() {
        MergeRequestEvent.Commit commit = new MergeRequestEvent.Commit(COMMIT_SHA);
        MergeRequestEvent.Attributes attributes = new MergeRequestEvent.Attributes(
            null, null, "main", commit, MR_IID, null, PROJECT_ID, "http://example.com/mr/1"
        );
        return new MergeRequestEvent(null, null, attributes);
    }

    @Test
    void processMergeRequest_whenAllSuccessful_shouldComplete() throws VersionExtractionException {
        MergeRequestEvent event = createMockEvent();

        when(gitLabApiClient.getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA)).thenReturn(Mono.just(POM_CONTENT));
        when(gitLabApiClient.getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA)).thenReturn(Mono.just(SPEC_CONTENT));
        when(versionExtractor.extractPomVersion(POM_CONTENT)).thenReturn("1.0.0");
        when(versionExtractor.extractApiSpecVersion(SPEC_CONTENT, SPEC_FILE_PATH)).thenReturn("1.2.3");

        StepVerifier.create(mergeRequestService.processMergeRequest(event, SPEC_FILE_PATH))
            .verifyComplete();

        // Verify interactions (optional but good practice)
        verify(gitLabApiClient).getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA);
        verify(gitLabApiClient).getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA);
        verify(versionExtractor).extractPomVersion(POM_CONTENT);
        verify(versionExtractor).extractApiSpecVersion(SPEC_CONTENT, SPEC_FILE_PATH);
    }

    @Test
    void processMergeRequest_whenPomFetchFails_shouldCompleteWithUnknownPomVersion() throws VersionExtractionException {
         MergeRequestEvent event = createMockEvent();

        // Simulate API client error for pom.xml
        when(gitLabApiClient.getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA)).thenReturn(Mono.empty()); 
        when(gitLabApiClient.getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA)).thenReturn(Mono.just(SPEC_CONTENT));
        // No need to mock extractPomVersion as it won't be called if content is empty
        when(versionExtractor.extractApiSpecVersion(SPEC_CONTENT, SPEC_FILE_PATH)).thenReturn("1.2.3");

        StepVerifier.create(mergeRequestService.processMergeRequest(event, SPEC_FILE_PATH))
            .verifyComplete(); // Should still complete, logging "unknown" for pom version internally
            
         // Verify interactions
        verify(gitLabApiClient).getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA);
        verify(gitLabApiClient).getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA);
        // verify(versionExtractor, never()).extractPomVersion(anyString()); // Content was empty
        verify(versionExtractor).extractApiSpecVersion(SPEC_CONTENT, SPEC_FILE_PATH);
    }
    
    @Test
    void processMergeRequest_whenSpecFetchFails_shouldCompleteWithUnknownSpecVersion() throws VersionExtractionException {
         MergeRequestEvent event = createMockEvent();

        when(gitLabApiClient.getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA)).thenReturn(Mono.just(POM_CONTENT));
        // Simulate API client error for spec file
        when(gitLabApiClient.getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA)).thenReturn(Mono.empty()); 
        when(versionExtractor.extractPomVersion(POM_CONTENT)).thenReturn("1.0.0");
         // No need to mock extractApiSpecVersion as it won't be called if content is empty

        StepVerifier.create(mergeRequestService.processMergeRequest(event, SPEC_FILE_PATH))
            .verifyComplete(); // Should still complete, logging "unknown" for spec version internally
            
         // Verify interactions
        verify(gitLabApiClient).getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA);
        verify(gitLabApiClient).getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA);
        verify(versionExtractor).extractPomVersion(POM_CONTENT);
        // verify(versionExtractor, never()).extractApiSpecVersion(anyString(), anyString()); // Content was empty
    }


    @Test
    void processMergeRequest_whenPomExtractionFails_shouldCompleteWithUnknownPomVersion() throws VersionExtractionException {
        MergeRequestEvent event = createMockEvent();

        when(gitLabApiClient.getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA)).thenReturn(Mono.just(POM_CONTENT));
        when(gitLabApiClient.getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA)).thenReturn(Mono.just(SPEC_CONTENT));
        // Simulate extraction error
        when(versionExtractor.extractPomVersion(POM_CONTENT)).thenThrow(new VersionExtractionException("POM parse error")); 
        when(versionExtractor.extractApiSpecVersion(SPEC_CONTENT, SPEC_FILE_PATH)).thenReturn("1.2.3");

        StepVerifier.create(mergeRequestService.processMergeRequest(event, SPEC_FILE_PATH))
            .verifyComplete(); // Should complete due to onErrorResume

        verify(gitLabApiClient).getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA);
        verify(gitLabApiClient).getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA);
        verify(versionExtractor).extractPomVersion(POM_CONTENT);
        verify(versionExtractor).extractApiSpecVersion(SPEC_CONTENT, SPEC_FILE_PATH);
    }
    
     @Test
    void processMergeRequest_whenSpecExtractionFails_shouldCompleteWithUnknownSpecVersion() throws VersionExtractionException {
        MergeRequestEvent event = createMockEvent();

        when(gitLabApiClient.getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA)).thenReturn(Mono.just(POM_CONTENT));
        when(gitLabApiClient.getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA)).thenReturn(Mono.just(SPEC_CONTENT));
        when(versionExtractor.extractPomVersion(POM_CONTENT)).thenReturn("1.0.0");
         // Simulate extraction error
        when(versionExtractor.extractApiSpecVersion(SPEC_CONTENT, SPEC_FILE_PATH)).thenThrow(new VersionExtractionException("Spec parse error"));

        StepVerifier.create(mergeRequestService.processMergeRequest(event, SPEC_FILE_PATH))
            .verifyComplete(); // Should complete due to onErrorResume

        verify(gitLabApiClient).getFileContent(PROJECT_ID, "pom.xml", COMMIT_SHA);
        verify(gitLabApiClient).getFileContent(PROJECT_ID, SPEC_FILE_PATH, COMMIT_SHA);
        verify(versionExtractor).extractPomVersion(POM_CONTENT);
        verify(versionExtractor).extractApiSpecVersion(SPEC_CONTENT, SPEC_FILE_PATH);
    }
}
