package vn.ctiep.jobhunter.controller;

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
    public String sendSimpleEmail() {
//         this.emailService.sendSimpleEmail();
//         this.emailService.sendEmailSync("ads.nctpulga29@gmail.com","test send email","<h1><b> Hello </n></h1>", false,true);
        //this.emailService.sendEmailFromTemplateSync("ads.nctpulga29@gmail.com","test send email","job", "Cong Tiep", this.);
        this.subscriberService.sendSubscribersEmailJobs();
        return "ok";
    }
}
