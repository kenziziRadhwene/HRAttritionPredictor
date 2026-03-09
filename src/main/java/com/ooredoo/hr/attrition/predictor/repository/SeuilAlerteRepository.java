package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.SeuilAlerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeuilAlerteRepository extends JpaRepository<SeuilAlerte, Long> {

    List<SeuilAlerte> findByActifTrue();
    Optional<SeuilAlerte> findByNom(String nom);
}