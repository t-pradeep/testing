package com.test.demo.webhook.gitlab.client; // Correct package

import com.test.demo.webhook.gitlab.dto.ApiResponses.MergeRequestChanges; // Correct DTO import
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Client interface for interacting with the GitLab API.
 */
public interface GitLabApiClient {

    /**
     * Fetches the changes for a given merge request.
     *
     * @param projectId        The ID of the target project.
     * @param mergeRequestIid The IID of the merge request.
     * @return A Mono emitting the merge request changes, or empty if not found or error.
     */
    Mono<MergeRequestChanges> getMergeRequestChanges(Long projectId, Long mergeRequestIid); // Correct return type

    /**
     * Fetches the raw content of a file from the repository at a specific ref (commit SHA, branch, tag).
     *
     * @param projectId The ID of the project.
     * @param filePath  The path to the file within the repository.
     * @param ref       The commit SHA, branch name, or tag name.
     * @return A Mono emitting the raw file content as a String, or empty if not found or error.
     */
    Mono<String> getFileContent(Long projectId, String filePath, String ref);

}
