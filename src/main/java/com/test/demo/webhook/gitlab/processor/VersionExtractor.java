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

    public String extractPomVersion(String pomContent) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
            new ByteArrayInputStream(pomContent.getBytes(StandardCharsets.UTF_8)))) {
            
            Model model = pomReader.read(reader);
            String version = model.getVersion();
            return version != null ? version : 
                   model.getParent() != null ? model.getParent().getVersion() : 
                   UNKNOWN_VERSION;
        } catch (IOException | XmlPullParserException e) {
            throw new Exception("Failed to parse pom.xml: " + e.getMessage(), e);
        }
    }

    public String extractApiSpecVersion(String specContent, String filePath) throws Exception {
        try {
            JsonNode rootNode = yamlMapper.readTree(specContent);
            JsonNode infoNode = rootNode.path(YAML_INFO_FIELD);
            
            if (infoNode.isMissingNode()) {
                throw new Exception("Missing 'info' field in API spec");
            }
            
            JsonNode versionNode = infoNode.path(YAML_VERSION_FIELD);
            if (versionNode.isMissingNode() || !versionNode.isTextual()) {
                throw new Exception("Missing or invalid version field in API spec");
            }
            
            return versionNode.asText();
        } catch (IOException e) {
            throw new Exception("Failed to parse API spec YAML: " + e.getMessage(), e);
        }
    }
}
