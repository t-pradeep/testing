package com.test.demo.webhook.gitlab.processor;

import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import com.test.demo.webhook.gitlab.service.MergeRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MergeRequestProcessorTest {

    @Mock
    private MergeRequestValidator validator;

    @Mock
    private FileChangeAnalyzer changeAnalyzer;

    @Mock
    private MergeRequestService mergeRequestService;

    @InjectMocks
    private MergeRequestProcessor mergeRequestProcessor;

    private MergeRequestEvent createMockEvent() {
        // Create a basic event structure sufficient for testing the processor flow
         MergeRequestEvent.Commit commit = new MergeRequestEvent.Commit("sha123");
         MergeRequestEvent.Attributes attributes = new MergeRequestEvent.Attributes(
            "merged", "merge", "main", commit, 1L, 100L, 200L, "url"
         );
         return new MergeRequestEvent("merge_request", "merge_request", attributes);
    }

    @Test
    void processEvent_whenValidationFails_shouldCompleteEmpty() {
        MergeRequestEvent event = createMockEvent();
        when(validator.validate(event)).thenReturn(false);

        StepVerifier.create(mergeRequestProcessor.processEvent(event))
            .verifyComplete();

        verify(validator).validate(event);
        verifyNoInteractions(changeAnalyzer, mergeRequestService);
    }

    @Test
    void processEvent_whenAnalyzerFindsNoChanges_shouldCompleteEmpty() {
        MergeRequestEvent event = createMockEvent();
        when(validator.validate(event)).thenReturn(true);
        when(changeAnalyzer.findChangedApiSpecFiles(event)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(mergeRequestProcessor.processEvent(event))
            .verifyComplete();

        verify(validator).validate(event);
        verify(changeAnalyzer).findChangedApiSpecFiles(event);
        verifyNoInteractions(mergeRequestService);
    }
    
    @Test
    void processEvent_whenAnalyzerReturnsEmptyMono_shouldCompleteEmpty() {
        MergeRequestEvent event = createMockEvent();
        when(validator.validate(event)).thenReturn(true);
        when(changeAnalyzer.findChangedApiSpecFiles(event)).thenReturn(Mono.empty()); // Analyzer itself returns empty

        StepVerifier.create(mergeRequestProcessor.processEvent(event))
            .verifyComplete();

        verify(validator).validate(event);
        verify(changeAnalyzer).findChangedApiSpecFiles(event);
        verifyNoInteractions(mergeRequestService);
    }


    @Test
    void processEvent_whenValidAndChangesFoundAndServiceSucceeds_shouldComplete() {
        MergeRequestEvent event = createMockEvent();
        String specFile = "spec/api.yaml";
        when(validator.validate(event)).thenReturn(true);
        when(changeAnalyzer.findChangedApiSpecFiles(event)).thenReturn(Mono.just(List.of(specFile, "other.yaml"))); // Multiple changes, processor takes first
        when(mergeRequestService.processMergeRequest(event, specFile)).thenReturn(Mono.empty());

        StepVerifier.create(mergeRequestProcessor.processEvent(event))
            .verifyComplete();

        verify(validator).validate(event);
        verify(changeAnalyzer).findChangedApiSpecFiles(event);
        verify(mergeRequestService).processMergeRequest(event, specFile);
    }

    @Test
    void processEvent_whenServiceFails_shouldCompleteWithErrorLogged() {
         MergeRequestEvent event = createMockEvent();
        String specFile = "spec/api.yaml";
        RuntimeException serviceError = new RuntimeException("Service failure");

        when(validator.validate(event)).thenReturn(true);
        when(changeAnalyzer.findChangedApiSpecFiles(event)).thenReturn(Mono.just(List.of(specFile)));
        when(mergeRequestService.processMergeRequest(event, specFile)).thenReturn(Mono.error(serviceError));

        StepVerifier.create(mergeRequestProcessor.processEvent(event))
             // Expect completion because the error is handled by doOnError and then() swallows it
            .verifyComplete(); 

        verify(validator).validate(event);
        verify(changeAnalyzer).findChangedApiSpecFiles(event);
        verify(mergeRequestService).processMergeRequest(event, specFile);
        // Verification of logging is complex with static loggers, but the flow confirms error handling path was taken.
    }
}
