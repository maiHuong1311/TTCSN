package com.learnflow.backend.prediction.dto;

public record PredictionSummaryResponse(long total, long completed, long failed, long ckdRisk, long normal) {}
