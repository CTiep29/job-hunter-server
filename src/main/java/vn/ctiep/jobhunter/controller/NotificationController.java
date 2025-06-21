package vn.ctiep.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.ctiep.jobhunter.service.WebSocketService;
import vn.ctiep.jobhunter.domain.response.NotificationDTO;
import vn.ctiep.jobhunter.util.SecurityUtil;
import vn.ctiep.jobhunter.util.annotation.ApiMessage;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final WebSocketService webSocketService;

    public NotificationController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @GetMapping("/unread")
    @ApiMessage("Get unread notifications")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        Long userId = SecurityUtil.getCurrentUserInsideToken()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<NotificationDTO> notifications = webSocketService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/mark-as-read")
    @ApiMessage("Mark notifications as read")
    public ResponseEntity<Void> markNotificationsAsRead() {
        Long userId = SecurityUtil.getCurrentUserInsideToken()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        webSocketService.markNotificationsAsRead(userId);
        return ResponseEntity.ok().build();
    }
}