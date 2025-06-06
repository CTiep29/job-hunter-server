package vn.ctiep.jobhunter.controller;

import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.domain.response.ResultPaginationDTO;
import vn.ctiep.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.ctiep.jobhunter.domain.response.job.ResUpdateJobDTO;
import vn.ctiep.jobhunter.service.JobService;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;
import vn.ctiep.jobhunter.util.constant.JobStatusEnum;
import vn.ctiep.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/jobs")
    @ApiMessage("Create a job")
    public ResponseEntity<ResCreateJobDTO> create(@Valid @RequestBody Job job) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.jobService.create(job));
    }

    @PutMapping("/jobs")
    @ApiMessage("Update a job")
    public ResponseEntity<ResUpdateJobDTO> update(@Valid @RequestBody Job job) throws IdInvalidException {
        Optional<Job> currentJob = this.jobService.fetchJobById(job.getId());
        if (!currentJob.isPresent()) {
            throw new IdInvalidException("Job not found");
        }

        return ResponseEntity.ok()
                .body(this.jobService.update(job, currentJob.get()));
    }

    @DeleteMapping("/jobs/{id}")
    @ApiMessage("Delete a job by id")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Job> currentJob = this.jobService.fetchJobById(id);
        if (!currentJob.isPresent()) {
            throw new IdInvalidException("Job not found");
        }
        this.jobService.handleDeleteJob(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/jobs/{id}")
    @ApiMessage("Get a job by id")
    public ResponseEntity<Job> getJob(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Job> currentJob = this.jobService.fetchJobById(id);
        if (!currentJob.isPresent()) {
            throw new IdInvalidException("Job not found");
        }

        return ResponseEntity.ok().body(currentJob.get());
    }
    @GetMapping("/jobs")
    @ApiMessage("Get job with pagination")
    public ResponseEntity<ResultPaginationDTO> getAllJob(
            @Filter Specification<Job> spec,
            Pageable pageable) {
        return ResponseEntity.ok().body(this.jobService.fetchAll(spec, pageable));
    }
    @GetMapping("/companies/{companyId}/jobs")
    @ApiMessage("Get jobs by company id for recruiter")
    public ResponseEntity<ResultPaginationDTO> getJobsByCompanyId(
            @PathVariable("companyId") long companyId,
            @Filter Specification<Job> spec,
            Pageable pageable) {

        return ResponseEntity.ok().body(this.jobService.fetchByCompanyId(companyId, spec, pageable));
    }

    @PutMapping("/jobs/{id}/restore")
    @ApiMessage("Restore a soft-deleted job")
    public ResponseEntity<Job> restoreJob(@PathVariable long id) {
        Job restoredJob = this.jobService.restoreJob(id);
        if (restoredJob != null) {
            return ResponseEntity.ok(restoredJob);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/jobs/{id}/approve")
    @ApiMessage("Approve a pending job")
    public ResponseEntity<Job> approveJob(@PathVariable long id) throws IdInvalidException {
        Optional<Job> currentJob = this.jobService.fetchJobById(id);
        if (!currentJob.isPresent()) {
            throw new IdInvalidException("Job not found");
        }
        
        Job approvedJob = this.jobService.approveJob(id);
        if (approvedJob != null) {
            return ResponseEntity.ok(approvedJob);
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/jobs/{id}/reject")
    @ApiMessage("Reject a pending job")
    public ResponseEntity<Job> rejectJob(@PathVariable long id) throws IdInvalidException {
        Optional<Job> currentJob = this.jobService.fetchJobById(id);
        if (!currentJob.isPresent()) {
            throw new IdInvalidException("Job not found");
        }
        
        Job rejectedJob = this.jobService.rejectJob(id);
        if (rejectedJob != null) {
            return ResponseEntity.ok(rejectedJob);
        }
        return ResponseEntity.badRequest().build();
    }
    @GetMapping("/jobs/count-pending")
    @ApiMessage("Count pending jobs")
    public ResponseEntity<?> countPendingJobs() {
        try {
            long count = jobService.countByStatus(JobStatusEnum.PENDING);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error counting pending jobs: " + e.getMessage());
        }
    }
}