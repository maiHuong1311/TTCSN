package com.learnflow.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalAdminBootstrap implements CommandLineRunner {

    private final AuthService users;
    private final String email, password;

    public LocalAdminBootstrap(
        AuthService s,
        @Value("${LOCAL_ADMIN_EMAIL:}") String e,
        @Value("${LOCAL_ADMIN_PASSWORD:}") String p
    ) {
        users = s;
        email = e;
        password = p;
    }

    public void run(String... args) {
        if (!email.isBlank() && !password.isBlank()) users.bootstrapAdmin(email, password);
    }
}
