package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.UploadUrlRequest;
import com.mottu.ops.application.dto.UploadUrlResponse;
import com.mottu.ops.application.port.out.StoragePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadUrlServiceTest {

    @Mock
    private StoragePort storagePort;

    @InjectMocks
    private UploadUrlService uploadUrlService;

    @Test
    @DisplayName("generateUploadUrl delegates to StoragePort with correct parameters")
    void generateUploadUrlDelegatesToStoragePort() {
        UUID inspectionId = UUID.randomUUID();
        UploadUrlRequest request = new UploadUrlRequest("image/webp", 250_000L, inspectionId, "front");
        UploadUrlResponse expected = new UploadUrlResponse("http://localhost:19000/test", "key.webp", 900);

        when(storagePort.generatePresignedUploadUrl("image/webp", 250_000L, inspectionId, "front"))
                .thenReturn(expected);

        UploadUrlResponse actual = uploadUrlService.generateUploadUrl(request);

        assertNotNull(actual);
        assertEquals(expected.uploadUrl(), actual.uploadUrl());
        assertEquals(expected.photoObjectKey(), actual.photoObjectKey());
        verify(storagePort).generatePresignedUploadUrl("image/webp", 250_000L, inspectionId, "front");
    }
}
