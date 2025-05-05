package vn.ctiep.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.ctiep.jobhunter.repository.CompanyRepository;
import vn.ctiep.jobhunter.repository.JobRepository;
import vn.ctiep.jobhunter.repository.UserRepository;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
public class DashboardController {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public DashboardController(JobRepository jobRepository, CompanyRepository companyRepository,
                               UserRepository userRepository){
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    @ApiMessage("Statistical data")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        long totalJobs = jobRepository.count();
        long totalCompanies = companyRepository.count();
        long totalUsers = userRepository.count();

        Map<String, Long> result = new HashMap<>();
        result.put("jobs", totalJobs);
        result.put("companies", totalCompanies);
        result.put("users", totalUsers);

        return ResponseEntity.ok(result);
    }
}

