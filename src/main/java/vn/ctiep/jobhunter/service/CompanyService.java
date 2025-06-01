package vn.ctiep.jobhunter.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import vn.ctiep.jobhunter.domain.Company;
import vn.ctiep.jobhunter.domain.User;
import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.util.annotation.OnUpdate;
import vn.ctiep.jobhunter.domain.response.ResultPaginationDTO;
import vn.ctiep.jobhunter.repository.CompanyRepository;
import vn.ctiep.jobhunter.repository.UserRepository;
import vn.ctiep.jobhunter.repository.JobRepository;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Validated
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository, JobRepository jobRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
    }

    public Company handleCreateCompany(Company c) {
        return this.companyRepository.save(c);
    }

    public ResultPaginationDTO handleGetCompany(Specification<Company> spec, Pageable pageable) {
        Page<Company> pageCompany = this.companyRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageCompany.getTotalPages());
        mt.setTotal(pageCompany.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageCompany.getContent());
        return rs;
    }

    @Validated(OnUpdate.class)
    public Company handleUpdateCompany(Company c) {
        Optional<Company> optionalCompany = this.companyRepository.findById(c.getId());
        if (optionalCompany.isPresent()) {
            Company currentCompany = optionalCompany.get();
            currentCompany.setLogo(c.getLogo());
            currentCompany.setName(c.getName());
            currentCompany.setDescription(c.getDescription());
            currentCompany.setAddress(c.getAddress());
            currentCompany.setTaxCode(c.getTaxCode());
            currentCompany.setUrl(c.getUrl());
            return this.companyRepository.save(currentCompany);
        }
        return null;
    }

    @Transactional
    public void handleDeleteCompany(long id) {
        Optional<Company> comOptional = this.companyRepository.findById(id);
        if (comOptional.isPresent()) {
            Company com = comOptional.get();
            List<Job> jobsToUpdate = new ArrayList<>();
            List<User> usersToUpdate = new ArrayList<>();
            
            // 1. Xóa mềm tất cả Jobs của company
            if (com.getJobs() != null) {
                for (Job job : com.getJobs()) {
                    job.setActive(false);
                    jobsToUpdate.add(job);
                }
                if (!jobsToUpdate.isEmpty()) {
                    jobRepository.saveAll(jobsToUpdate);
                    entityManager.flush();
                }
            }
            
            // 2. Xóa mềm tất cả Users của company
            List<User> users = this.userRepository.findByCompany(com);
            for (User user : users) {
                user.setActive(false);
                usersToUpdate.add(user);
            }
            if (!usersToUpdate.isEmpty()) {
                userRepository.saveAll(usersToUpdate);
                entityManager.flush();
            }
            
            // 3. Xóa mềm company
            companyRepository.updateActiveStatus(id, false);
            entityManager.flush();
        }
    }

    public Optional<Company> findById(long id) {
        return this.companyRepository.findById(id);
    }

    @Transactional
    public Company restoreCompany(long id) {
        Optional<Company> companyOptional = this.companyRepository.findByIdAndActiveFalse(id);
        if (companyOptional.isPresent()) {
            Company company = companyOptional.get();
            List<Job> jobsToUpdate = new ArrayList<>();
            List<User> usersToUpdate = new ArrayList<>();
            
            // 1. Khôi phục tất cả Jobs của company
            if (company.getJobs() != null) {
                for (Job job : company.getJobs()) {
                    job.setActive(true);
                    jobsToUpdate.add(job);
                }
                if (!jobsToUpdate.isEmpty()) {
                    jobRepository.saveAll(jobsToUpdate);
                }
            }
            
            // 2. Khôi phục tất cả Users của company
            List<User> users = this.userRepository.findByCompany(company);
            for (User user : users) {
                user.setActive(true);
                usersToUpdate.add(user);
            }
            if (!usersToUpdate.isEmpty()) {
                userRepository.saveAll(usersToUpdate);
            }
            
            // 3. Khôi phục company
            company.setActive(true);
            return this.companyRepository.save(company);
        }
        return null;
    }
}
