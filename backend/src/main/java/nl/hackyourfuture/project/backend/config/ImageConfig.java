package nl.hackyourfuture.project.backend.config;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageConfig {

    @Bean
    public ImageKitClient imageKitClient(
            @Value("${image.private-key}") String privateKey
    ) {
        return ImageKitOkHttpClient.builder()
                .privateKey(privateKey)
                .build();
    }
}
