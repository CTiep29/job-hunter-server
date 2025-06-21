package vn.ctiep.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ctiep.jobhunter.domain.Notification;
import vn.ctiep.jobhunter.domain.User;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserAndReadOrderByTimestampDesc(User user, boolean read);
    long countByUserAndRead(User user, boolean read);
}
