package com.learnflow.backend.ai;

import com.learnflow.backend.prediction.dto.PredictionRequest;

public interface AIProvider {
    AiPrediction predict(PredictionRequest request);
}
