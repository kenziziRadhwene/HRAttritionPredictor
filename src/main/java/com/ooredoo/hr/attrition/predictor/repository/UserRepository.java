package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.User;
import com.ooredoo.hr.attrition.predictor.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    @Query("SELECT u.email FROM User u WHERE u.userRole = :role")
    List<String> findAllResponsableRHEmails(@Param("role") ERole role);

    @Query("SELECT u.email FROM User u WHERE u.userRole = 'MANAGER' AND u.departement = :departement")
    Optional<String> findManagerEmailByDepartement(@Param("departement") String departement);


    List<User> findByUserRole(ERole userRole);

 
}