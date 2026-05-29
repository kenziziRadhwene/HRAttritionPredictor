package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.AuditLog;
import com.ooredoo.hr.attrition.predictor.enums.EAuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByAction(EAuditAction action);
    List<AuditLog> findByTargetTableAndTargetId(String targetTable, Long targetId);
    List<AuditLog> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :debut AND a.createdAt < :fin")
    List<AuditLog> findByCreatedAtBetween(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );

    // ─── Requête filtrée + paginée ────────────────────────────
    @Query("""
            SELECT a FROM AuditLog a
            WHERE
              (:role IS NULL OR :role = '' OR
                (a.user IS NOT NULL AND CAST(a.user.userRole AS string) = :role) OR
                (:role = '-' AND a.user IS NULL))
              AND
              (:action IS NULL OR :action = '' OR CAST(a.action AS string) = :action)
              AND
              (:search IS NULL OR :search = '' OR
                LOWER(CONCAT(COALESCE(a.user.nom, ''), ' ', COALESCE(a.user.prenom, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(COALESCE(a.user.email, ''))   LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(COALESCE(a.ipAddress, ''))    LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(COALESCE(a.details, ''))      LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findFiltered(
            @Param("search") String search,
            @Param("role")   String role,
            @Param("action") String action,
            Pageable pageable
    );
}