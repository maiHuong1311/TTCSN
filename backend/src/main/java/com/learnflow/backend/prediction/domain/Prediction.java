package com.learnflow.backend.prediction.domain;

import com.learnflow.backend.ai.AiPrediction;
import com.learnflow.backend.auth.domain.AppUser;
import com.learnflow.backend.prediction.dto.PredictionRequest;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity @Table(name = "prediction")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prediction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private AppUser user;
    @Column(name = "patient_code") private String patientCode;
    private Integer age; private BigDecimal bp;
    private String htn; private String dm; private String cad; private String appet; private String pe; private String ane;
    private BigDecimal sg; private Integer al; private Integer su;
    private String rbc; private String pc; private String pcc; private String ba;
    private BigDecimal bgr; private BigDecimal bu; private BigDecimal sc; private BigDecimal sod; private BigDecimal pot;
    private BigDecimal hemo; private BigDecimal pcv; private BigDecimal wc; private BigDecimal rc;
    private String status; private String outcome;
    @Column(name = "risk_score") private BigDecimal riskScore;
    @Column(name = "model_version") private String modelVersion;
    @Column(name = "failure_reason") private String failureReason;
    @Column(name = "created_at") private Instant createdAt;

    public static Prediction pending(AppUser user, PredictionRequest r, Instant now) {
        Prediction p = new Prediction(); p.user=user; p.patientCode=r.patientCode(); p.age=r.age(); p.bp=r.bp(); p.htn=r.htn(); p.dm=r.dm(); p.cad=r.cad();
        p.appet=r.appet(); p.pe=r.pe(); p.ane=r.ane(); p.sg=r.sg(); p.al=r.al(); p.su=r.su(); p.rbc=r.rbc(); p.pc=r.pc(); p.pcc=r.pcc(); p.ba=r.ba();
        p.bgr=r.bgr(); p.bu=r.bu(); p.sc=r.sc(); p.sod=r.sod(); p.pot=r.pot(); p.hemo=r.hemo(); p.pcv=r.pcv(); p.wc=r.wc(); p.rc=r.rc();
        p.status="PENDING"; p.createdAt=now; return p;
    }
    public void complete(AiPrediction result) { status="COMPLETED"; outcome=result.outcome(); riskScore=result.riskScore(); modelVersion=result.modelVersion(); failureReason=null; }
    public void fail(String reason) { status="FAILED"; failureReason=reason == null ? "AI prediction failed" : reason.substring(0, Math.min(reason.length(), 500)); }
}
