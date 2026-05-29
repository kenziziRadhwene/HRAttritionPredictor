package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.ImportSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportSessionRepository extends JpaRepository<ImportSession, Long> {
    List<ImportSession> findAllByOrderByCreatedAtDesc();
    List<ImportSession> findByUploadedById(Long userId);
}