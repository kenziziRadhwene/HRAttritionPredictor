package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByDateCreationDesc(Long userId);

    List<Notification> findByUserIdAndLueFalse(Long userId);

    long countByUserIdAndLueFalse(Long userId);

    boolean existsByUserIdAndMoisRecalcul(Long userId, String moisRecalcul);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.user.id = :userId AND n.lue = false")
    void marquerToutesLues(@Param("userId") Long userId);
}