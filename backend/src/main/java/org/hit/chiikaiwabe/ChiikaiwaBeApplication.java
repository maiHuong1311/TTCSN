package org.hit.chiikaiwabe;

import org.hit.chiikaiwabe.config.properties.AdminInfoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties({AdminInfoProperties.class})
@SpringBootApplication
public class ChiikaiwaBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChiikaiwaBeApplication.class, args);
    }

}
