package com.learnflow.backend.prediction;

import com.learnflow.backend.common.web.PageResponse;
import com.learnflow.backend.prediction.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {
    private final PredictionService service; private final PredictionBatchParser parser;
    public PredictionController(PredictionService service, PredictionBatchParser parser) { this.service=service; this.parser=parser; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PredictionResponse predict(Authentication auth, @Valid @RequestBody PredictionRequest request) { return service.predict((Long)auth.getPrincipal(), request); }

    @PostMapping(value="/batch", consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public List<PredictionResponse> batch(Authentication auth, @RequestPart("file") MultipartFile file) { return service.predictBatch((Long)auth.getPrincipal(), parser.parse(file)); }

    @GetMapping
    public PageResponse<PredictionResponse> history(Authentication auth, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return service.history((Long)auth.getPrincipal(),page,size); }

    @GetMapping("/summary")
    public PredictionSummaryResponse summary(Authentication auth) { return service.summary((Long)auth.getPrincipal()); }
}
