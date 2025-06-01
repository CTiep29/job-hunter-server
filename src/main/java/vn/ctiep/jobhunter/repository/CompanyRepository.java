package vn.ctiep.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.ctiep.jobhunter.domain.Company;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

    List<Company> findByActiveTrue();
    
    // Thêm phương thức tìm company đã bị xóa mềm
    Optional<Company> findByIdAndActiveFalse(Long id);

    @Modifying
    @Query("UPDATE Company c SET c.active = :active WHERE c.id = :id")
    void updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}
