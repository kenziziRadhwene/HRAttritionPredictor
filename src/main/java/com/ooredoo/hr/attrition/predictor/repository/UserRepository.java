package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    @Query("SELECT u.email FROM User u WHERE u.userRole = 'RESPONSABLE_RH'")
    List<String> findAllResponsableRHEmails();
}