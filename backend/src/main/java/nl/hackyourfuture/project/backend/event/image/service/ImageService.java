package nl.hackyourfuture.project.backend.event.image.service;

import io.imagekit.client.ImageKitClient;
import io.imagekit.errors.ImageKitException;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.event.image.exceptions.ImageUploadException;
import nl.hackyourfuture.project.backend.event.image.repository.EventImageRepository;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {
    private final EventImageRepository eventImageRepository;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ImageKitClient imageKitClient;

    private String validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Please provide an image");
        }

        String contentType = image.getContentType();

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Image must be a JPEG, PNG, or WebP file"
            );
        }

        return contentType;
    }

    private String getFileExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BadRequestException(
                    "Image must be a JPEG, PNG, or WebP file"
            );
        };
    }

    public String upload(UUID eventId, MultipartFile image) {
        String contentType = validateImage(image);

        String fileName = UUID.randomUUID()
                + getFileExtension(contentType);

        try (InputStream imageContent = image.getInputStream()) {
            FileUploadParams params = FileUploadParams.builder()
                    .file(imageContent)
                    .fileName(fileName)
                    .folder("/events")
                    .useUniqueFileName(true)
                    .build();

            FileUploadResponse response = imageKitClient.files().upload(params);

            String imageUrl = response.url()
                    .orElseThrow(() -> {
                        log.error(
                                "ImageKit upload returned no URL for event {}",
                                eventId
                        );
                        return new ImageUploadException(
                                "Image upload did not return a URL"
                        );
                    });

            eventImageRepository.save(eventId, imageUrl, contentType);

            log.info("Uploaded image for event {}", eventId);

            return imageUrl;
        } catch (IOException | ImageKitException exception) {
            log.error(
                    "ImageKit upload failed for event {}",
                    eventId,
                    exception
            );
            throw new ImageUploadException(
                    "Unable to upload the image",
                    exception
            );
        }
    }
}
