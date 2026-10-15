package org.moa.global.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageException;
import com.google.firebase.cloud.StorageClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FirebaseStorageImageTest {
    @Test
    void absentFileAndConcurrentNotFoundAreIdempotent() {
        Bucket bucket = mock(Bucket.class);
        StorageClient client = mock(StorageClient.class);
        FirebaseStorageService service = new FirebaseStorageService();
        ReflectionTestUtils.setField(service, "bucketName", "isolated-test");
        try (MockedStatic<StorageClient> clients = mockStatic(StorageClient.class)) {
            clients.when(StorageClient::getInstance).thenReturn(client);
            when(client.bucket("isolated-test")).thenReturn(bucket);
            assertThat(service.deleteFile("absent.jpg")).isTrue();
            Blob blob = mock(Blob.class);
            when(bucket.get("race.jpg")).thenReturn(blob, (Blob) null);
            when(blob.delete()).thenReturn(false);
            assertThat(service.deleteFile("race.jpg")).isTrue();
            when(bucket.get("not-found.jpg")).thenThrow(new StorageException(404, "missing"));
            assertThat(service.deleteFile("not-found.jpg")).isTrue();
            when(bucket.get("denied.jpg")).thenThrow(new StorageException(403, "denied"));
            assertThat(service.deleteFile("denied.jpg")).isFalse();
        }
    }

    @Test
    void uploadedNameAndContentAreReturnedWithoutSubsequentStorageCalls() throws Exception {
        Bucket bucket = mock(Bucket.class);
        StorageClient client = mock(StorageClient.class);
        FirebaseStorageService service = new FirebaseStorageService();
        ReflectionTestUtils.setField(service, "bucketName", "isolated-test");
        try (MockedStatic<StorageClient> clients = mockStatic(StorageClient.class)) {
            clients.when(StorageClient::getInstance).thenReturn(client);
            when(client.bucket("isolated-test")).thenReturn(bucket);
            when(bucket.create(anyString(), any(InputStream.class), eq("image/png"))).thenAnswer(i -> {
                InputStream stream = i.getArgument(1);
                assertThat(stream.readAllBytes()).containsExactly((byte) 1, (byte) 2);
                return mock(Blob.class);
            });
            String name = service.uploadProcessed(new byte[]{1, 2}, "png", "image/png");
            assertThat(name).matches("[0-9a-f-]{36}\\.png");
            verify(bucket).create(eq(name), any(InputStream.class), eq("image/png"));
            verifyNoMoreInteractions(bucket);
        }
    }
}
