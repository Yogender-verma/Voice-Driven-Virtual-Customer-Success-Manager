package com.vcsm.controller;

import com.vcsm.service.VoiceModelStorageService;
import com.vcsm.service.VoiceModelRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VoiceModelControllerTest {

    @Test
    void uploadVoiceModelStoresZipArchive() {
        VoiceModelStorageService storageService = new FakeVoiceModelStorageService();
        VoiceModelRegistryService registryService = new VoiceModelRegistryService();
        VoiceModelController controller = new VoiceModelController(storageService, registryService);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "enterprise-model.zip",
                "application/zip",
                "zip-content".getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = controller.uploadVoiceModel(file);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("voice-models/test-model.zip", registryService.getActiveModel().orElseThrow().modelKey());
    }

    @Test
    void uploadVoiceModelRejectsNonZipArchive() {
        VoiceModelController controller = new VoiceModelController(
                new FakeVoiceModelStorageService(),
                new VoiceModelRegistryService()
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "voice-model.txt",
                "text/plain",
                "not-a-zip".getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<?> response = controller.uploadVoiceModel(file);

        assertEquals(400, response.getStatusCode().value());
    }

    private static class FakeVoiceModelStorageService extends VoiceModelStorageService {
        FakeVoiceModelStorageService() {
            super(null, "test-bucket", "voice-models");
        }

        @Override
        public StoredVoiceModel store(org.springframework.web.multipart.MultipartFile file) {
            return new StoredVoiceModel("test-bucket", "voice-models/test-model.zip");
        }
    }
}
