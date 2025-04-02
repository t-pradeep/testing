package com.test.demo.webhook.gitlab.processor;

import com.test.demo.webhook.gitlab.dto.MergeRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MergeRequestValidatorTest {

    private MergeRequestValidator validator;
    private final Set<String> targetBranches = Set.of("main", "develop");

    @BeforeEach
    void setUp() {
        validator = new MergeRequestValidator(targetBranches);
    }

    private MergeRequestEvent createValidEvent(String action, String targetBranch) {
        MergeRequestEvent.Commit commit = new MergeRequestEvent.Commit("sha123456789");
        MergeRequestEvent.Attributes attributes = new MergeRequestEvent.Attributes(
            "merged", // state (doesn't matter for validation)
            action,
            targetBranch,
            commit,
            1L, // iid
            100L, // sourceProjectId (doesn't matter)
            200L, // targetProjectId
            "http://example.com/mr/1" // url (doesn't matter)
        );
        return new MergeRequestEvent("merge_request", "merge_request", attributes);
    }

    @Test
    void validate_whenValidEvent_shouldReturnTrue() {
        MergeRequestEvent event = createValidEvent("merge", "main");
        assertTrue(validator.validate(event));
    }

    @Test
    void validate_whenEventIsNull_shouldReturnFalse() {
        assertFalse(validator.validate(null));
    }
    
    @Test
    void validate_whenObjectKindIsNotMergeRequest_shouldReturnFalse() {
         MergeRequestEvent event = new MergeRequestEvent("issue", "issue", null); // Invalid kind
         assertFalse(validator.validate(event));
    }

    @Test
    void validate_whenAttributesNull_shouldReturnFalse() {
         MergeRequestEvent event = new MergeRequestEvent("merge_request", "merge_request", null);
         assertFalse(validator.validate(event));
    }
    
    @Test
    void validate_whenLastCommitNull_shouldReturnFalse() {
        MergeRequestEvent.Attributes attributes = new MergeRequestEvent.Attributes(
            "merged", "merge", "main", null, 1L, 100L, 200L, "url"
        );
        MergeRequestEvent event = new MergeRequestEvent("merge_request", "merge_request", attributes);
        assertFalse(validator.validate(event));
    }
    
    @Test
    void validate_whenCommitIdNullOrBlank_shouldReturnFalse() {
         MergeRequestEvent.Commit commit = new MergeRequestEvent.Commit(null); // Null ID
         MergeRequestEvent.Attributes attributes = new MergeRequestEvent.Attributes(
            "merged", "merge", "main", commit, 1L, 100L, 200L, "url"
         );
         MergeRequestEvent event = new MergeRequestEvent("merge_request", "merge_request", attributes);
         assertFalse(validator.validate(event));

         commit = new MergeRequestEvent.Commit(""); // Blank ID
         attributes = new MergeRequestEvent.Attributes(
            "merged", "merge", "main", commit, 1L, 100L, 200L, "url"
         );
         event = new MergeRequestEvent("merge_request", "merge_request", attributes);
         assertFalse(validator.validate(event));
    }
    
    @Test
    void validate_whenTargetProjectIdNull_shouldReturnFalse() {
         MergeRequestEvent.Commit commit = new MergeRequestEvent.Commit("sha123");
         MergeRequestEvent.Attributes attributes = new MergeRequestEvent.Attributes(
            "merged", "merge", "main", commit, 1L, 100L, null, "url" // Null target project ID
         );
         MergeRequestEvent event = new MergeRequestEvent("merge_request", "merge_request", attributes);
         assertFalse(validator.validate(event));
    }
    
     @Test
    void validate_whenIidNull_shouldReturnFalse() {
         MergeRequestEvent.Commit commit = new MergeRequestEvent.Commit("sha123");
         MergeRequestEvent.Attributes attributes = new MergeRequestEvent.Attributes(
            "merged", "merge", "main", commit, null, 100L, 200L, "url" // Null IID
         );
         MergeRequestEvent event = new MergeRequestEvent("merge_request", "merge_request", attributes);
         assertFalse(validator.validate(event));
    }

    @ParameterizedTest
    @ValueSource(strings = {"open", "close", "reopen", "update", ""})
    @NullSource
    void validate_whenActionIsNotMerge_shouldReturnFalse(String action) {
        MergeRequestEvent event = createValidEvent(action, "main");
        assertFalse(validator.validate(event));
    }

    @Test
    void validate_whenTargetBranchNotInList_shouldReturnFalse() {
        MergeRequestEvent event = createValidEvent("merge", "feature-branch");
        assertFalse(validator.validate(event));
    }
    
    @Test
    void validate_whenTargetBranchIsNull_shouldReturnFalse() {
        MergeRequestEvent event = createValidEvent("merge", null);
        assertFalse(validator.validate(event));
    }

    @Test
    void validate_whenTargetBranchesIsEmpty_shouldReturnFalse() {
        validator = new MergeRequestValidator(Set.of()); // Empty set
        MergeRequestEvent event = createValidEvent("merge", "main");
        assertFalse(validator.validate(event));
    }
}
