package vn.ctiep.jobhunter.service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.domain.Skill;
import vn.ctiep.jobhunter.domain.Subscriber;
import vn.ctiep.jobhunter.domain.response.email.ResEmailJob;
import vn.ctiep.jobhunter.repository.JobRepository;
import vn.ctiep.jobhunter.repository.SkillRepository;
import vn.ctiep.jobhunter.repository.SubscriberRepository;

@Service
public class SubscriberService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SubscriberRepository subscriberRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final EmailService emailService;

    public SubscriberService(
            SubscriberRepository subscriberRepository,
            SkillRepository skillRepository,
            JobRepository jobRepository,
            EmailService emailService,
            RedisTemplate<String, Object> redisTemplate) {
        this.subscriberRepository = subscriberRepository;
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    public boolean isExistsByEmail(String email) {
        return this.subscriberRepository.existsByEmail(email);
    }

    public Subscriber create(Subscriber subs) {
        // check skills
        if (subs.getSkills() != null) {
            List<Long> reqSkills = subs.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            subs.setSkills(dbSkills);
        }

        return this.subscriberRepository.save(subs);
    }

    public Subscriber update(Subscriber subsDB, Subscriber subsRequest) {
        // check skills
        if (subsRequest.getSkills() != null) {
            List<Long> reqSkills = subsRequest.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            subsDB.setSkills(dbSkills);
        }
        return this.subscriberRepository.save(subsDB);
    }

    public Subscriber findById(long id) {
        Optional<Subscriber> subsOptional = this.subscriberRepository.findById(id);
        if (subsOptional.isPresent())
            return subsOptional.get();
        return null;
    }
    @Transactional
    public void delete(Long id) {
        Optional<Subscriber> subscriberOptional = this.subscriberRepository.findById(id);
        if (subscriberOptional.isPresent()) {
            Subscriber subscriber = subscriberOptional.get();

            // Xóa mối liên hệ giữa subscriber và skill (bảng subscriber_skill)
            subscriber.getSkills().forEach(skill -> skill.getSubscribers().remove(subscriber));
            subscriber.getSkills().clear();

            // Xóa subscriber
            this.subscriberRepository.delete(subscriber);
        } else {
            throw new EntityNotFoundException("Không tìm thấy subscriber với id = " + id);
        }
    }

    public ResEmailJob convertJobToSendEmail(Job job) {
        ResEmailJob res = new ResEmailJob();
        res.setName(job.getName());
        res.setSalary(job.getSalary());
        res.setCompany(new ResEmailJob.CompanyEmail(job.getCompany().getName()));
        List<Skill> skills = job.getSkills();
        List<ResEmailJob.SkillEmail> s = skills.stream().map(skill -> new ResEmailJob.SkillEmail(skill.getName()))
                .collect(Collectors.toList());
        res.setSkills(s);
        return res;
    }

    @SuppressWarnings("unchecked")
    private List<Job> filterNewJobs(String email, List<Job> jobs) {
        String redisKey = "sent_jobs:" + email;

        Set<Long> cache = (Set<Long>) redisTemplate.opsForValue().get(redisKey);
        Set<Long> sentJobIds = (cache != null) ? cache : new HashSet<>();

        return jobs.stream()
                .filter(job -> !sentJobIds.contains(job.getId()))
                .toList();
    }

    @SuppressWarnings("unchecked")
        private void markJobsAsSent(String email, List<Job> jobs) {
            String redisKey = "sent_jobs:" + email;

            Set<Long> sentJobIds = (Set<Long>) redisTemplate.opsForValue().get(redisKey);
            if (sentJobIds == null) {
                sentJobIds = new HashSet<>();
            }

            for (Job job : jobs) {
                sentJobIds.add(job.getId());
            }

            redisTemplate.opsForValue().set(redisKey, sentJobIds, Duration.ofDays(30));
        }

    public int sendSubscribersEmailJobs() {
        int sent = 0;
        List<Subscriber> listSubs = this.subscriberRepository.findAll();
        for (Subscriber sub : listSubs) {
            List<Skill> listSkills = sub.getSkills();
            if (listSkills == null || listSkills.isEmpty()) continue;

            List<Job> matchedJobs = this.jobRepository.findBySkillsInAndActiveTrue(listSkills);
            List<Job> newJobs = filterNewJobs(sub.getEmail(), matchedJobs);

            if (!newJobs.isEmpty()) {
                List<ResEmailJob> arr = newJobs.stream()
                        .map(this::convertJobToSendEmail)
                        .collect(Collectors.toList());

                this.emailService.sendEmailFromTemplateSync(
                        sub.getEmail(),
                        "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                        "job",
                        sub.getName(),
                        arr);

                markJobsAsSent(sub.getEmail(), newJobs);
                sent++;
            }
        }
        return sent;
    }



    public Subscriber findByEmail(String email) {
        return this.subscriberRepository.findByEmail(email);
    }
}
