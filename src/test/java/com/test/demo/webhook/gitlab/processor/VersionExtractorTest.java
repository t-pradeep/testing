package com.test.demo.webhook.gitlab.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*; // Ensure doThrow is imported

@ExtendWith(MockitoExtension.class)
class VersionExtractorTest {

    @Mock
    private ObjectMapper yamlMapper;

    @Mock
    private MavenXpp3Reader pomReader;

    @InjectMocks
    private VersionExtractor versionExtractor;

    private final String pomContent = "<project><version>1.0.0</version></project>";
    private final String pomWithParentContent = "<project><parent><version>2.0.0</version></parent></project>";
    private final String pomNoVersionContent = "<project></project>";
    private final String yamlContent = "info:\n  version: 1.2.3";
    private final String yamlNoInfoContent = "other: data";
    private final String yamlNoVersionContent = "info:\n  title: API";
    private final String invalidYamlContent = "info: version: 1.2.3"; // Invalid YAML syntax

    @Test
    void extractPomVersion_whenVersionPresent_shouldReturnVersion() throws Exception {
        Model model = new Model();
        model.setVersion("1.0.0");
        when(pomReader.read(any(Reader.class))).thenReturn(model);

        assertEquals("1.0.0", versionExtractor.extractPomVersion(pomContent));
    }

    @Test
    void extractPomVersion_whenOnlyParentVersionPresent_shouldReturnParentVersion() throws Exception {
        Model model = new Model();
        Parent parent = new Parent();
        parent.setVersion("2.0.0");
        model.setParent(parent);
        when(pomReader.read(any(Reader.class))).thenReturn(model);

        assertEquals("2.0.0", versionExtractor.extractPomVersion(pomWithParentContent));
    }

    @Test
    void extractPomVersion_whenNoVersionPresent_shouldReturnUnknown() throws Exception {
        Model model = new Model(); // No version, no parent
        when(pomReader.read(any(Reader.class))).thenReturn(model);

        assertEquals("unknown", versionExtractor.extractPomVersion(pomNoVersionContent));
    }

    @Test
    void extractPomVersion_whenPomReaderThrowsIOException_shouldThrowVersionExtractionException() throws Exception {
        when(pomReader.read(any(Reader.class))).thenThrow(new IOException("Read error"));

        assertThrows(VersionExtractionException.class, () -> versionExtractor.extractPomVersion(pomContent));
    }

    @Test
    void extractPomVersion_whenPomReaderThrowsXmlPullParserException_shouldThrowVersionExtractionException() throws Exception {
        when(pomReader.read(any(Reader.class))).thenThrow(new XmlPullParserException("Parse error"));

        assertThrows(VersionExtractionException.class, () -> versionExtractor.extractPomVersion(pomContent));
    }

    @Test
    void extractApiSpecVersion_whenVersionPresent_shouldReturnVersion() throws Exception {
        JsonNode rootNode = mock(JsonNode.class);
        JsonNode infoNode = mock(JsonNode.class);
        JsonNode versionNode = mock(JsonNode.class);

        when(yamlMapper.readTree(yamlContent)).thenReturn(rootNode);
        when(rootNode.path("info")).thenReturn(infoNode);
        when(infoNode.isMissingNode()).thenReturn(false);
        when(infoNode.path("version")).thenReturn(versionNode);
        when(versionNode.isMissingNode()).thenReturn(false);
        when(versionNode.isTextual()).thenReturn(true);
        when(versionNode.asText()).thenReturn("1.2.3");

        assertEquals("1.2.3", versionExtractor.extractApiSpecVersion(yamlContent, "spec.yaml"));
    }

    @Test
    void extractApiSpecVersion_whenInfoMissing_shouldThrowVersionExtractionException() throws Exception {
        JsonNode rootNode = mock(JsonNode.class);
        JsonNode missingNode = mock(JsonNode.class);

        when(yamlMapper.readTree(yamlNoInfoContent)).thenReturn(rootNode);
        when(rootNode.path("info")).thenReturn(missingNode);
        when(missingNode.isMissingNode()).thenReturn(true);

        assertThrows(VersionExtractionException.class, () -> versionExtractor.extractApiSpecVersion(yamlNoInfoContent, "spec.yaml"));
    }

    @Test
    void extractApiSpecVersion_whenVersionMissing_shouldThrowVersionExtractionException() throws Exception {
        JsonNode rootNode = mock(JsonNode.class);
        JsonNode infoNode = mock(JsonNode.class);
        JsonNode missingNode = mock(JsonNode.class);

        when(yamlMapper.readTree(yamlNoVersionContent)).thenReturn(rootNode);
        when(rootNode.path("info")).thenReturn(infoNode);
        when(infoNode.isMissingNode()).thenReturn(false);
        when(infoNode.path("version")).thenReturn(missingNode);
        when(missingNode.isMissingNode()).thenReturn(true);

        assertThrows(VersionExtractionException.class, () -> versionExtractor.extractApiSpecVersion(yamlNoVersionContent, "spec.yaml"));
    }
    
     @Test
    void extractApiSpecVersion_whenVersionNotTextual_shouldThrowVersionExtractionException() throws Exception {
        JsonNode rootNode = mock(JsonNode.class);
        JsonNode infoNode = mock(JsonNode.class);
        JsonNode nonTextualVersionNode = mock(JsonNode.class);

        when(yamlMapper.readTree(yamlNoVersionContent)).thenReturn(rootNode); // Use content that would parse
        when(rootNode.path("info")).thenReturn(infoNode);
        when(infoNode.isMissingNode()).thenReturn(false);
        when(infoNode.path("version")).thenReturn(nonTextualVersionNode);
        when(nonTextualVersionNode.isMissingNode()).thenReturn(false);
        when(nonTextualVersionNode.isTextual()).thenReturn(false); // Version is not text

        assertThrows(VersionExtractionException.class, () -> versionExtractor.extractApiSpecVersion(yamlNoVersionContent, "spec.yaml"));
    }


    @Test
    void extractApiSpecVersion_whenYamlMapperThrowsIOException_shouldThrowVersionExtractionException() throws IOException {
        // Use thenThrow with a RuntimeException wrapping the IOException as a workaround for Mockito
        when(yamlMapper.readTree(invalidYamlContent)).thenThrow(new RuntimeException(new IOException("Parse error")));

        // Expect VersionExtractionException because the code catches IOException and wraps it
        VersionExtractionException thrown = assertThrows(VersionExtractionException.class, () -> {
            versionExtractor.extractApiSpecVersion(invalidYamlContent, "spec.yaml");
        });
        
        // Optionally assert the cause if needed, though the type assertion is primary
        assertNotNull(thrown.getCause());
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("Parse error", thrown.getCause().getMessage());
    }
}
