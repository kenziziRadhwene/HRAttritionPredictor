package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.ScoreRisque;
import com.ooredoo.hr.attrition.predictor.enums.ENiveauRisque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRisqueRepository extends JpaRepository<ScoreRisque, Long> {

    List<ScoreRisque> findByEmployeeIdOrderByDateCalculDesc(Long employeeId);
    Optional<ScoreRisque> findTopByEmployeeIdOrderByDateCalculDesc(Long employeeId);
    List<ScoreRisque> findByNiveauRisque(ENiveauRisque niveauRisque);
}