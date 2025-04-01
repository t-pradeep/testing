package com.test.demo.webhook.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MergeRequestEvent(
    @JsonProperty("object_kind") String objectKind,
    @JsonProperty("event_type") String eventType,
    @JsonProperty("object_attributes") Attributes attributes
) {
    public record Attributes(
        @JsonProperty("state") String state,
        @JsonProperty("action") String action,
        @JsonProperty("target_branch") String targetBranch,
        @JsonProperty("last_commit") Commit lastCommit,
        @JsonProperty("iid") Long iid,
        @JsonProperty("source_project_id") Long sourceProjectId,
        @JsonProperty("target_project_id") Long targetProjectId,
        @JsonProperty("url") String url
    ) {}

    public record Commit(
        @JsonProperty("id") String id
    ) {}
}
