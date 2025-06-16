package shop.ink3.api.common.uploader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import shop.ink3.api.common.exception.MinioUploadFailException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class MinioServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private MinioService uploader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Minio 이미지 업로드 성공")
    void uploadSuccess() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test image".getBytes()
        );

        doReturn(null).when(s3Client)
            .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        String result = uploader.upload(file, "review-bucket");

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(result).startsWith(today + "/");
    }

    @Test
    @DisplayName("Minio 이미지 업로드 실패")
    void uploadFail() throws IOException {
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenThrow(IOException.class);
        when(file.getSize()).thenReturn(10L);

        assertThatThrownBy(() -> uploader.upload(file, "bucket"))
            .isInstanceOf(MinioUploadFailException.class)
            .hasMessageContaining("MinIO 파일 업로드 실패");
    }

    @Test
    @DisplayName("Presigned URL 가져오기 - prefix 변환 포함")
    void getPresignedUrl() {
        String key = "20250602/test.jpg";
        String bucket = "review-bucket";
        String fakeUrl = "http://storage.java21.net:8000/" + key;

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(constructFakeUrl(fakeUrl));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
            .thenReturn(presignedRequest);

        String url = uploader.getPresignedUrl(key, bucket);
        assertThat(url).isEqualTo("/image-proxy/" + key);
    }

    @Test
    @DisplayName("Minio 객체 삭제")
    void deleteSuccess() {
        uploader.delete("20250602/test.jpg", "review-bucket");
        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    private URL constructFakeUrl(String urlString) {
        try {
            return new URL(urlString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
