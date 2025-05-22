package vn.ctiep.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.ctiep.jobhunter.domain.Company;
import vn.ctiep.jobhunter.domain.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    User findByEmail(String email);

    boolean existsByEmail(String emmail);

    User findByRefreshTokenAndEmail(String token, String email);

    List<User> findByCompany(Company company);

    List<User> findByCompanyAndActiveTrue(Company company);

    Optional<User> findByIdAndActiveFalse(Long id);

    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count " +
           "FROM users " +
           "WHERE (:startDate IS NULL OR created_at >= :startDate) " +
           "AND (:endDate IS NULL OR created_at <= :endDate) " +
           "GROUP BY DATE_FORMAT(created_at, '%Y-%m') " +
           "ORDER BY month", nativeQuery = true)
    List<Map<String, Object>> countNewUsersByMonth(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

}
