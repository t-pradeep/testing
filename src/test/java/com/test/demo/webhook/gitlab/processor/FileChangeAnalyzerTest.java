package com.test.demo.webhook.gitlab.processor;

import com.test.demo.webhook.gitlab.client.GitLabApiClient;
import com.test.demo.webhook.gitlab.dto.ApiResponses;
import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileChangeAnalyzerTest {

    @Mock
    private GitLabApiClient gitLabApiClient;

    private FileChangeAnalyzer fileChangeAnalyzer;

    private final Set<String> apiSpecFiles = Set.of("spec/api.yaml", "other/spec.json");

    @BeforeEach
    void setUp() {
        // InjectMocks doesn't work well with constructor injection of Collections/Sets
        // Initialize manually
        fileChangeAnalyzer = new FileChangeAnalyzer(apiSpecFiles, gitLabApiClient);
    }

    private MergeRequestEvent createMockEvent() {
        // Only need attributes relevant to the analyzer
        MergeRequestEvent.Attributes attributes = new MergeRequestEvent.Attributes(
            null, null, null, null, 123L, null, 456L, null
        );
        return new MergeRequestEvent(null, null, attributes);
    }

    @Test
    void findChangedApiSpecFiles_whenApiSpecFilesIsEmpty_shouldReturnEmptyMono() {
        fileChangeAnalyzer = new FileChangeAnalyzer(Collections.emptySet(), gitLabApiClient); // Use empty set
        MergeRequestEvent event = createMockEvent();

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
            .expectNext(Collections.emptyList())
            .verifyComplete();
    }

    @Test
    void findChangedApiSpecFiles_whenNoChanges_shouldReturnEmptyMono() {
        MergeRequestEvent event = createMockEvent();
        ApiResponses.MergeRequestChanges apiResponse = new ApiResponses.MergeRequestChanges(null); // No changes list

        when(gitLabApiClient.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(Mono.just(apiResponse));

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
            .expectNext(Collections.emptyList())
            .verifyComplete();
    }
    
     @Test
    void findChangedApiSpecFiles_whenChangesIsNull_shouldReturnEmptyMono() {
        MergeRequestEvent event = createMockEvent();
        ApiResponses.MergeRequestChanges apiResponse = new ApiResponses.MergeRequestChanges(null); // Null changes list

        when(gitLabApiClient.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(Mono.just(apiResponse));

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
            .expectNext(Collections.emptyList())
            .verifyComplete();
    }


    @Test
    void findChangedApiSpecFiles_whenChangesExistButNoneMatch_shouldReturnEmptyMono() {
        MergeRequestEvent event = createMockEvent();
        List<ApiResponses.MergeRequestChanges.Change> changes = List.of(
            new ApiResponses.MergeRequestChanges.Change("old/path.txt", "new/path.txt", false, false, false),
            new ApiResponses.MergeRequestChanges.Change("README.md", "README.md", false, false, false)
        );
        ApiResponses.MergeRequestChanges apiResponse = new ApiResponses.MergeRequestChanges(changes);

        when(gitLabApiClient.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(Mono.just(apiResponse));

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
            .expectNext(Collections.emptyList())
            .verifyComplete();
    }

    @Test
    void findChangedApiSpecFiles_whenOneChangeMatches_shouldReturnListOfOne() {
        MergeRequestEvent event = createMockEvent();
        List<ApiResponses.MergeRequestChanges.Change> changes = List.of(
            new ApiResponses.MergeRequestChanges.Change("old/path.txt", "new/path.txt", false, false, false),
            new ApiResponses.MergeRequestChanges.Change("spec/api.yaml", "spec/api.yaml", false, false, false) // Match
        );
        ApiResponses.MergeRequestChanges apiResponse = new ApiResponses.MergeRequestChanges(changes);

        when(gitLabApiClient.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(Mono.just(apiResponse));

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
            .expectNext(List.of("spec/api.yaml"))
            .verifyComplete();
    }

    @Test
    void findChangedApiSpecFiles_whenMultipleChangesMatch_shouldReturnListOfAllMatches() {
        MergeRequestEvent event = createMockEvent();
        List<ApiResponses.MergeRequestChanges.Change> changes = List.of(
            new ApiResponses.MergeRequestChanges.Change("old/path.txt", "new/path.txt", false, false, false),
            new ApiResponses.MergeRequestChanges.Change("spec/api.yaml", "spec/api.yaml", false, false, false), // Match 1
            new ApiResponses.MergeRequestChanges.Change("other/spec.json", "other/spec.json", false, false, false) // Match 2
        );
        ApiResponses.MergeRequestChanges apiResponse = new ApiResponses.MergeRequestChanges(changes);

        when(gitLabApiClient.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(Mono.just(apiResponse));

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
            // Order might not be guaranteed by stream, check contents
            .expectNextMatches(list -> list.containsAll(List.of("spec/api.yaml", "other/spec.json")) && list.size() == 2)
            .verifyComplete();
    }
    
     @Test
    void findChangedApiSpecFiles_whenDuplicateMatches_shouldReturnDistinctList() {
        MergeRequestEvent event = createMockEvent();
        List<ApiResponses.MergeRequestChanges.Change> changes = List.of(
            new ApiResponses.MergeRequestChanges.Change("spec/api.yaml", "spec/api.yaml", false, false, false), // Match 1
            new ApiResponses.MergeRequestChanges.Change("spec/api.yaml", "spec/api.yaml", false, false, false) // Duplicate Match
        );
        ApiResponses.MergeRequestChanges apiResponse = new ApiResponses.MergeRequestChanges(changes);

        when(gitLabApiClient.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(Mono.just(apiResponse));

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
            .expectNext(List.of("spec/api.yaml")) // Only one distinct entry
            .verifyComplete();
    }


    @Test
    void findChangedApiSpecFiles_whenApiClientReturnsEmpty_shouldReturnEmptyList() {
        MergeRequestEvent event = createMockEvent();
        when(gitLabApiClient.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
             // .defaultIfEmpty(Collections.emptyList()) handles the empty Mono case
            .expectNext(Collections.emptyList())
            .verifyComplete();
    }
    
    @Test
    void findChangedApiSpecFiles_whenChangeHasNullPaths_shouldHandleGracefully() {
        MergeRequestEvent event = createMockEvent();
         List<ApiResponses.MergeRequestChanges.Change> changes = List.of(
            new ApiResponses.MergeRequestChanges.Change(null, null, false, false, false), // Both null
            new ApiResponses.MergeRequestChanges.Change("spec/api.yaml", "spec/api.yaml", false, false, false) // Match
        );
        ApiResponses.MergeRequestChanges apiResponse = new ApiResponses.MergeRequestChanges(changes);

        when(gitLabApiClient.getMergeRequestChanges(anyLong(), anyLong())).thenReturn(Mono.just(apiResponse));

        StepVerifier.create(fileChangeAnalyzer.findChangedApiSpecFiles(event))
            .expectNext(List.of("spec/api.yaml")) // Should ignore the null path change
            .verifyComplete();
    }
}
