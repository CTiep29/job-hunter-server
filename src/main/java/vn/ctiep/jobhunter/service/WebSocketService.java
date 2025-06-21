package vn.ctiep.jobhunter.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.ctiep.jobhunter.domain.Notification;
import vn.ctiep.jobhunter.domain.User;
import vn.ctiep.jobhunter.domain.response.NotificationDTO;
import vn.ctiep.jobhunter.repository.NotificationRepository;
import vn.ctiep.jobhunter.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WebSocketService {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public WebSocketService(SimpMessagingTemplate messagingTemplate,
                            NotificationRepository notificationRepository,
                            UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void sendNotificationToUser(Long userId, NotificationDTO notificationDTO) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Lưu notification vào database
            Notification notification = new Notification();
            notification.setType(notificationDTO.getType());
            notification.setMessage(notificationDTO.getMessage());
            notification.setJobName(notificationDTO.getJobName());
            notification.setCompanyName(notificationDTO.getCompanyName());
            notification.setResumeId(notificationDTO.getResumeId());
            notification.setUser(user);
            notification.setTimestamp(notificationDTO.getTimestamp());
            notification.setRead(false);

            notificationRepository.save(notification);

            // Gửi notification realtime
            String destination = "/user/" + userId + "/queue/notifications";
            messagingTemplate.convertAndSend(destination, notificationDTO);

            logger.info("Notification saved and sent to user {}", userId);
        } catch (Exception e) {
            logger.error("Error sending notification to user {}: {}", userId, e.getMessage());
        }
    }

    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> notifications = notificationRepository.findByUserAndReadOrderByTimestampDesc(user, false);
        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markNotificationsAsRead(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> notifications = notificationRepository.findByUserAndReadOrderByTimestampDesc(user, false);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setType(notification.getType());
        dto.setMessage(notification.getMessage());
        dto.setJobName(notification.getJobName());
        dto.setCompanyName(notification.getCompanyName());
        dto.setResumeId(notification.getResumeId());
        dto.setUserId(notification.getUser().getId());
        dto.setTimestamp(notification.getTimestamp());
        return dto;
    }
}