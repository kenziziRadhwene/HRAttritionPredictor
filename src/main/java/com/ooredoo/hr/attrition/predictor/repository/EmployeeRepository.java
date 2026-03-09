package com.ooredoo.hr.attrition.predictor.repository;

import com.ooredoo.hr.attrition.predictor.entity.Employee;
import com.ooredoo.hr.attrition.predictor.enums.EDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByMatricule(String matricule);
    List<Employee> findByDepartment(EDepartment department);
    List<Employee> findByActiveTrue();
    boolean existsByEmail(String email);
    boolean existsByMatricule(String matricule);
}