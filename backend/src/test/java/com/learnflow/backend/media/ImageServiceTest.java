package com.learnflow.backend.media;

import static org.junit.jupiter.api.Assertions.*;

import com.learnflow.backend.common.web.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageServiceTest {

    @TempDir
    Path directory;

    @Test
    void storesActualImageWithGeneratedNameAndCanReadAfterServiceRestart() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(5, 7, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var service = new ImageService(directory.toString());
        var result = service.upload(
            new MockMultipartFile("file", "../../danger.png", "text/plain", bytes.toByteArray())
        );
        assertTrue(result.imageUrl().matches("/api/images/[a-f0-9-]+\\.png"));
        String name = result.imageUrl().substring("/api/images/".length());
        try (var stream = new ImageService(directory.toString()).read(name).getInputStream()) {
            assertEquals(7, ImageIO.read(stream).getHeight());
        }
    }

    @Test
    void rejectsFakeImagesLargeFilesAndTraversal() {
        var service = new ImageService(directory.toString());
        assertThrows(ApiException.class, () ->
            service.upload(
                new MockMultipartFile("file", "fake.png", "image/png", "<script>fake</script>".getBytes())
            )
        );
        assertThrows(ApiException.class, () ->
            service.upload(new MockMultipartFile("file", new byte[5 * 1024 * 1024 + 1]))
        );
        assertThrows(ApiException.class, () -> service.read("../application.yml"));
        assertThrows(ApiException.class, () -> service.read("00000000-0000-0000-0000-000000000000.png"));
    }
}
