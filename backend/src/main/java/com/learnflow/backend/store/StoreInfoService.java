package com.learnflow.backend.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class StoreInfoService {

    public record Contact(
        String hotline,
        String email,
        String address,
        String supportHours,
        String timezone
    ) {}

    public record Policy(String id, String title, List<String> paragraphs) {}

    public record Info(Contact contact, List<Policy> policies) {}

    private final Info info;

    public StoreInfoService(ObjectMapper mapper) throws java.io.IOException {
        try (
            var stream = new ClassPathResource(
                "fashion/store-info.json"
            ).getInputStream()
        ) {
            info = mapper.readValue(stream, Info.class);
        }
    }

    public Info info() {
        return info;
    }

    public String contactText() {
        Contact c = info.contact();
        return (
            "Hotline: " +
            c.hotline() +
            ". Email: " +
            c.email() +
            ". Địa chỉ: " +
            c.address() +
            ". Hỗ trợ: " +
            c.supportHours() +
            "."
        );
    }

    public Optional<String> answer(String message) {
        String t = java.text.Normalizer.normalize(
            message.toLowerCase(Locale.ROOT),
            java.text.Normalizer.Form.NFD
        )
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd')
            .toLowerCase(Locale.ROOT);
        List<String> ids = new ArrayList<>();
        if (
            contains(t, "ship", "van chuyen", "giao hang", "giao cham")
        ) ids.add("shipping");
        if (
            contains(
                t,
                "doi tra",
                "tra hang",
                "doi hang",
                "doi size",
                "hoan hang"
            )
        ) ids.add("returns");
        if (contains(t, "thanh toan", "chuyen khoan", "cod", "qr")) ids.add(
            "payments"
        );
        if (contains(t, "bao mat", "du lieu", "thong tin ca nhan")) ids.add(
            "privacy"
        );
        boolean contact = contains(
            t,
            "lien he",
            "hotline",
            "email",
            "dia chi cua hang",
            "gio mo cua",
            "gio ho tro"
        );
        if (ids.isEmpty() && !contact) return Optional.empty();
        List<String> parts = info
            .policies()
            .stream()
            .filter(p -> ids.contains(p.id()))
            .map(p -> p.title() + ": " + String.join(" ", p.paragraphs()))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        parts.add(contactText());
        return Optional.of(String.join("\n\n", parts));
    }

    private boolean contains(String text, String... words) {
        return Arrays.stream(words).anyMatch(text::contains);
    }
}
