package vn.ctiep.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ctiep.jobhunter.repository.*;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;
import vn.ctiep.jobhunter.domain.Company;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("api/v1")
public class DashboardController {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    public DashboardController(JobRepository jobRepository, 
                             CompanyRepository companyRepository,
                             UserRepository userRepository,
                               ResumeRepository resumeRepository) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
    }

    @GetMapping("/stats")
    @ApiMessage("Statistical data")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        // Tổng quan
        result.put("totalJobs", jobRepository.count());
        result.put("totalCompanies", companyRepository.count());
        result.put("totalUsers", userRepository.count());

        // Thống kê công việc đang tuyển theo công ty
        result.put("activeJobsByCompany", jobRepository.countActiveJobsByCompany());
    
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/time-series")
    @ApiMessage("Time series statistics")
    public ResponseEntity<Map<String, Object>> getTimeSeriesStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        Map<String, Object> result = new HashMap<>();
        
        // Thống kê theo thời gian
        result.put("newJobsByMonth", jobRepository.countNewJobsByMonth(startDate, endDate));
        result.put("newUsersByMonth", userRepository.countNewUsersByMonth(startDate, endDate));
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/company/{companyId}")
    @ApiMessage("Company dashboard statistics")
    public ResponseEntity<Map<String, Object>> getCompanyStatistics(@PathVariable Long companyId) {
        Map<String, Object> result = new HashMap<>();
        
        // Thống kê cho nhà tuyển dụng
        result.put("totalJobs", jobRepository.countByCompanyId(companyId));
        
        // Lấy thông tin công ty
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("Company not found"));
            
        // Đếm số việc làm đang hoạt động
        result.put("activeJobs", jobRepository.findByCompanyAndActiveTrue(company).size());

        // Thống kê resumes
        Map<String, Object> resumeStats = new HashMap<>();
        
        // Tổng số resumes
        long totalResumes = resumeRepository.countByCompanyId(companyId);
        resumeStats.put("totalResumes", totalResumes);
        
        // Thống kê theo trạng thái
        List<Map<String, Object>> byStatus = resumeRepository.countResumesByStatus(companyId);
        resumeStats.put("byStatus", byStatus);
        
        // Thống kê theo công việc
        List<Map<String, Object>> byJob = resumeRepository.countResumesByJob(companyId);
        resumeStats.put("byJob", byJob);
        
        result.put("resumeStats", resumeStats);
        
        return ResponseEntity.ok(result);
    }
}

