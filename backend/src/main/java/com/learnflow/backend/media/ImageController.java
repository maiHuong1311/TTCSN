package com.learnflow.backend.media;

import com.learnflow.backend.media.dto.ImageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ImageController {

    private final ImageService service;

    public ImageController(ImageService service) {
        this.service = service;
    }

    @PostMapping(value = "/admin/product-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageResponse upload(@RequestParam("file") MultipartFile file) {
        return service.upload(file);
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        Resource image = service.read(filename);
        return ResponseEntity.ok()
            .contentType(filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
            .body(image);
    }
}
