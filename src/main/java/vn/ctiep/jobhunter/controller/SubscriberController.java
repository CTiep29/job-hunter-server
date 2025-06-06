package vn.ctiep.jobhunter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import vn.ctiep.jobhunter.domain.Subscriber;
import vn.ctiep.jobhunter.service.SubscriberService;
import vn.ctiep.jobhunter.util.SecurityUtil;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;
import vn.ctiep.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class SubscriberController {
    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping("/subscribers")
    @ApiMessage("Create a subscriber")
    public ResponseEntity<Subscriber> create(@Valid @RequestBody Subscriber sub) throws IdInvalidException {
        // check email
        boolean isExist = this.subscriberService.isExistsByEmail(sub.getEmail());
        if (isExist == true) {
            throw new IdInvalidException("Email " + sub.getEmail() + " đã tồn tại");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(this.subscriberService.create(sub));
    }

    @PutMapping("/subscribers")
    @ApiMessage("Update a subscriber")
    public ResponseEntity<Subscriber> update(@RequestBody Subscriber subsRequest) throws IdInvalidException {
        // check id
        Subscriber subsDB = this.subscriberService.findById(subsRequest.getId());
        if (subsDB == null) {
            throw new IdInvalidException("Id " + subsRequest.getId() + " không tồn tại");
        }
        return ResponseEntity.ok().body(this.subscriberService.update(subsDB, subsRequest));
    }

    @PostMapping("/subscribers/skills")
    @ApiMessage("Get subscriber's skill")
    public ResponseEntity<Subscriber> getSubscribersSkill() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() == true ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        return ResponseEntity.status(HttpStatus.CREATED).body(this.subscriberService.findByEmail(email));
    }
    @DeleteMapping("/subscribers/{id}")
    @ApiMessage("Delete a subscriber")
    public ResponseEntity<?> delete(@PathVariable("id") long id) throws IdInvalidException {
        Subscriber subs = this.subscriberService.findById(id);
        if (subs == null) {
            throw new IdInvalidException("Id " + id + " không tồn tại");
        }
        this.subscriberService.delete(id);
        return ResponseEntity.ok().build();
    }

}
