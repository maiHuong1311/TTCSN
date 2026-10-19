package com.learnflow.backend.health;

import com.learnflow.backend.health.dto.HealthResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "fashion-store-backend");
    }
}
