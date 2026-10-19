package com.learnflow.backend.prediction;

import com.learnflow.backend.prediction.domain.Prediction;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    Page<Prediction> findByUserId(Long userId, Pageable pageable);

    @Query("select p.status, count(p) from Prediction p where p.user.id = :userId group by p.status")
    List<Object[]> countByStatus(@Param("userId") Long userId);

    @Query("select p.outcome, count(p) from Prediction p where p.user.id = :userId and p.status = 'COMPLETED' group by p.outcome")
    List<Object[]> countCompletedByOutcome(@Param("userId") Long userId);
}
