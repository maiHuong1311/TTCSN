package com.learnflow.backend.prediction;

import com.learnflow.backend.ai.*;
import com.learnflow.backend.auth.AuthService;
import com.learnflow.backend.auth.domain.AppUser;
import com.learnflow.backend.common.web.*;
import com.learnflow.backend.prediction.domain.Prediction;
import com.learnflow.backend.prediction.dto.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PredictionService {
    private final PredictionRepository repository; private final AuthService authService; private final AIProvider aiProvider; private final Clock clock; private final Validator validator;
    public PredictionService(PredictionRepository repository, AuthService authService, AIProvider aiProvider, Clock clock, Validator validator) {
        this.repository=repository; this.authService=authService; this.aiProvider=aiProvider; this.clock=clock; this.validator=validator;
    }

    public PredictionResponse predict(Long userId, PredictionRequest request) {
        validate(request); AppUser user=authService.requireUser(userId); Prediction prediction=repository.save(Prediction.pending(user, request, clock.instant()));
        try { prediction.complete(aiProvider.predict(request)); }
        catch (AIProviderException exception) { prediction.fail(exception.getMessage()); }
        return response(repository.save(prediction));
    }

    public List<PredictionResponse> predictBatch(Long userId, List<PredictionRequest> requests) {
        return requests.stream().map(request -> predict(userId, request)).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PredictionResponse> history(Long userId, int page, int size) {
        if(page<0 || size<1 || size>100) throw new ApiException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be between 1 and 100");
        return PageResponse.from(repository.findByUserId(userId, PageRequest.of(page,size, Sort.by(Sort.Direction.DESC,"createdAt"))).map(this::response));
    }

    @Transactional(readOnly = true)
    public PredictionSummaryResponse summary(Long userId) {
        Map<String,Long> statuses=new HashMap<>(), outcomes=new HashMap<>();
        repository.countByStatus(userId).forEach(row -> statuses.put((String)row[0],(Long)row[1]));
        repository.countCompletedByOutcome(userId).forEach(row -> outcomes.put((String)row[0],(Long)row[1]));
        long completed=statuses.getOrDefault("COMPLETED",0L), failed=statuses.getOrDefault("FAILED",0L);
        return new PredictionSummaryResponse(completed+failed+statuses.getOrDefault("PENDING",0L), completed, failed, outcomes.getOrDefault("CKD_RISK",0L), outcomes.getOrDefault("NORMAL",0L));
    }

    private void validate(PredictionRequest request) { Set<ConstraintViolation<PredictionRequest>> errors=validator.validate(request); if(!errors.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, errors.iterator().next().getPropertyPath()+" "+errors.iterator().next().getMessage()); }
    private PredictionResponse response(Prediction p) { return new PredictionResponse(p.getId(),p.getPatientCode(),p.getStatus(),p.getOutcome(),p.getRiskScore(),p.getModelVersion(),p.getFailureReason(),p.getCreatedAt()); }
}
