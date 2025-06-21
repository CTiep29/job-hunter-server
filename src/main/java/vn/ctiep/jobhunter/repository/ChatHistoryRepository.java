package vn.ctiep.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ctiep.jobhunter.domain.ChatHistory;
import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    List<ChatHistory> findByUserIdOrderByTimestampDesc(String userId);
}