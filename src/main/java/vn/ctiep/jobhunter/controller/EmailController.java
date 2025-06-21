package vn.ctiep.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.ctiep.jobhunter.service.EmailService;
import vn.ctiep.jobhunter.service.SubscriberService;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1")
public class EmailController {

    private final EmailService emailService;
    private final SubscriberService subscriberService;

    public EmailController(EmailService emailService,
            SubscriberService subscriberService) {
        this.emailService = emailService;
        this.subscriberService = subscriberService;
    }

    @GetMapping("/email")
    @ApiMessage("Send simple email")
    @Scheduled(cron = "0 0 9 * * *") // gửi lúc 9h sáng hàng ngày
    @Transactional
    public ResponseEntity<?> sendSimpleEmail() {
        int sentCount = this.subscriberService.sendSubscribersEmailJobs();
        System.out.println("Run cronjob send email ....");
        return ResponseEntity.ok(sentCount);
    }

}
