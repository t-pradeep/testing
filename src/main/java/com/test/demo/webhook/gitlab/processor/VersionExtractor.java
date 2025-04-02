package com.test.demo.webhook.gitlab.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class VersionExtractor {
    private static final String UNKNOWN_VERSION = "unknown";
    private static final String YAML_INFO_FIELD = "info";
    private static final String YAML_VERSION_FIELD = "version";

    private final ObjectMapper yamlMapper;
    private final MavenXpp3Reader pomReader;

    public VersionExtractor(ObjectMapper yamlMapper, MavenXpp3Reader pomReader) {
        this.yamlMapper = yamlMapper;
        this.pomReader = pomReader;
    }

    public String extractPomVersion(String pomContent) throws VersionExtractionException { // Use custom exception
        try (InputStreamReader reader = new InputStreamReader(
            new ByteArrayInputStream(pomContent.getBytes(StandardCharsets.UTF_8)))) {

            Model model = pomReader.read(reader);
            String version = model.getVersion();
            // Handle potential null parent or parent version gracefully
            String parentVersion = (model.getParent() != null) ? model.getParent().getVersion() : null;
            
            if (version != null) {
                return version;
            } else if (parentVersion != null) {
                return parentVersion;
            } else {
                 // Explicitly return unknown if neither is found, avoid throwing exception for this case
                return UNKNOWN_VERSION;
            }
        } catch (IOException | XmlPullParserException e) {
            throw new VersionExtractionException("Failed to parse pom.xml: " + e.getMessage(), e); // Use custom exception
        }
    }

    public String extractApiSpecVersion(String specContent, String filePath) throws VersionExtractionException {
        try {
            JsonNode rootNode = yamlMapper.readTree(specContent);
            JsonNode infoNode = rootNode.path(YAML_INFO_FIELD);

            if (infoNode.isMissingNode()) {
                throw new VersionExtractionException("Missing 'info' field in API spec: " + filePath);
            }

            JsonNode versionNode = infoNode.path(YAML_VERSION_FIELD);
            if (versionNode.isMissingNode() || !versionNode.isTextual()) {
                 throw new VersionExtractionException("Missing or invalid 'version' field under 'info' in API spec: " + filePath);
            }

            return versionNode.asText();
        } catch (Exception e) {
            // Handle both direct IOExceptions and RuntimeExceptions wrapping IOExceptions
            Throwable cause = e;
            while (cause != null && !(cause instanceof IOException)) {
                cause = cause.getCause();
            }
            if (cause instanceof IOException) {
                throw new VersionExtractionException("Failed to parse API spec YAML '" + filePath + "': " + cause.getMessage(), (IOException) cause);
            }
            throw new VersionExtractionException("Unexpected error parsing API spec YAML '" + filePath + "': " + e.getMessage(), e);
        }
    }
}
