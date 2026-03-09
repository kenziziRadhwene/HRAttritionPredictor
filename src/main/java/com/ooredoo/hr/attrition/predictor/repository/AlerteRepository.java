package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.Alerte;
import com.ooredoo.hr.attrition.predictor.enums.EStatutAlerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    List<Alerte> findByStatut(EStatutAlerte statut);
    List<Alerte> findByEmployeeId(Long employeeId);
    List<Alerte> findByEmailEnvoyeFalse();
    long countByStatut(EStatutAlerte statut);
}