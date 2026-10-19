package com.learnflow.backend.media;

import com.learnflow.backend.common.web.ApiException;
import com.learnflow.backend.media.dto.ImageResponse;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {

    private final Path directory;

    public ImageService(@Value("${application.images.directory:./uploads/product-images}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public ImageResponse upload(MultipartFile file) {
        if (file.isEmpty()) throw invalid("Vui lòng chọn ảnh để tải lên.");
        if (file.getSize() > 5 * 1024 * 1024) throw invalid("Ảnh không được vượt quá 5 MB.");
        // Inspect actual image bytes; never use the original filename as a storage path.
        try (var source = file.getInputStream(); var input = ImageIO.createImageInputStream(source)) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("Chỉ hỗ trợ ảnh JPG và PNG hợp lệ.");
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg")) throw invalid(
                    "Chỉ hỗ trợ ảnh JPG và PNG."
                );
                reader.setInput(input, true, true);
                int width = reader.getWidth(0),
                    height = reader.getHeight(0);
                if (
                    width < 1 ||
                    height < 1 ||
                    width > 8192 ||
                    height > 8192 ||
                    (long) width * height > 20_000_000
                ) {
                    throw invalid("Ảnh quá lớn. Chọn ảnh tối đa 8192 px mỗi chiều và 20 triệu điểm ảnh.");
                }
                var image = reader.read(0);
                String extension = format.equals("jpeg") ? "jpg" : "png";
                String name = UUID.randomUUID() + "." + extension;
                Files.createDirectories(directory);
                Path target = directory.resolve(name);
                try {
                    if (!ImageIO.write(image, extension, target.toFile())) throw new IOException(
                        "Image writer unavailable"
                    );
                } catch (IOException ex) {
                    Files.deleteIfExists(target);
                    throw ex;
                }
                return new ImageResponse("/api/images/" + name);
            } finally {
                reader.dispose();
            }
        } catch (javax.imageio.IIOException ex) {
            throw invalid("Ảnh không hợp lệ hoặc bị hỏng. Vui lòng chọn ảnh khác.");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh. Vui lòng thử lại.");
        }
    }

    public Resource read(String filename) {
        if (!filename.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(png|jpg)")) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh.");
        }
        Path path = directory.resolve(filename).normalize();
        if (!path.startsWith(directory) || !Files.isRegularFile(path)) throw new ApiException(
            HttpStatus.NOT_FOUND,
            "Không tìm thấy ảnh."
        );
        return new PathResource(path);
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
