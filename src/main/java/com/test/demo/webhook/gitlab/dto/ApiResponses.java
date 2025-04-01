package com.test.demo.webhook.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ApiResponses {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MergeRequestChanges(
        @JsonProperty("changes") List<Change> changes
    ) {
        public record Change(
            @JsonProperty("old_path") String oldPath,
            @JsonProperty("new_path") String newPath,
            @JsonProperty("new_file") boolean newFile,
            @JsonProperty("renamed_file") boolean renamedFile,
            @JsonProperty("deleted_file") boolean deletedFile
        ) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FileContent(
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_path") String filePath,
        @JsonProperty("content") String content,
        @JsonProperty("encoding") String encoding
    ) {}
}
