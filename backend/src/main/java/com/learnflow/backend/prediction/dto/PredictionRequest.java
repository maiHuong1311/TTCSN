package com.learnflow.backend.prediction.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PredictionRequest(
    @Size(max = 80) String patientCode,
    @NotNull @Min(1) @Max(120) Integer age,
    @NotNull @DecimalMin("20") @DecimalMax("200") BigDecimal bp,
    @NotBlank @Pattern(regexp = "yes|no") String htn,
    @NotBlank @Pattern(regexp = "yes|no") String dm,
    @NotBlank @Pattern(regexp = "yes|no") String cad,
    @NotBlank @Pattern(regexp = "good|poor") String appet,
    @NotBlank @Pattern(regexp = "yes|no") String pe,
    @NotBlank @Pattern(regexp = "yes|no") String ane,
    @NotNull @DecimalMin("1.005") @DecimalMax("1.025") BigDecimal sg,
    @NotNull @Min(0) @Max(5) Integer al,
    @NotNull @Min(0) @Max(5) Integer su,
    @NotBlank @Pattern(regexp = "normal|abnormal") String rbc,
    @NotBlank @Pattern(regexp = "normal|abnormal") String pc,
    @NotBlank @Pattern(regexp = "present|notpresent") String pcc,
    @NotBlank @Pattern(regexp = "present|notpresent") String ba,
    @NotNull @PositiveOrZero BigDecimal bgr,
    @NotNull @PositiveOrZero BigDecimal bu,
    @NotNull @PositiveOrZero BigDecimal sc,
    @NotNull @PositiveOrZero BigDecimal sod,
    @NotNull @PositiveOrZero BigDecimal pot,
    @NotNull @PositiveOrZero BigDecimal hemo,
    @NotNull @PositiveOrZero BigDecimal pcv,
    @NotNull @PositiveOrZero BigDecimal wc,
    @NotNull @PositiveOrZero BigDecimal rc
) {}
