package com.learnflow.backend.ai;

import com.learnflow.backend.ai.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
public class FashionAIController {

    private final FashionAIService service;

    public FashionAIController(FashionAIService s) {
        service = s;
    }

    @GetMapping("/status")
    public AiStatusResponse status() {
        return service.status();
    }

    @PostMapping("/chat")
    public ChatResponse chat(Authentication a, @Valid @RequestBody ChatRequest r) {
        return service.chat((Long) a.getPrincipal(), r);
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    public SseEmitter stream(Authentication a, @Valid @RequestBody ChatRequest r) {
        Long user = (Long) a.getPrincipal();
        SseEmitter emitter = new SseEmitter(15000L);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("status").data("Đang xử lý"));
                emitter.send(SseEmitter.event().name("result").data(service.chat(user, r)));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }
}
