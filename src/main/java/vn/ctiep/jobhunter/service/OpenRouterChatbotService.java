package vn.ctiep.jobhunter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.ctiep.jobhunter.domain.ChatHistory;
import vn.ctiep.jobhunter.domain.ChatRequest;
import vn.ctiep.jobhunter.domain.ChatResponse;
import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.repository.ChatHistoryRepository;
import vn.ctiep.jobhunter.repository.JobRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenRouterChatbotService {
    private final RestTemplate restTemplate;
    private final ChatHistoryRepository chatHistoryRepository;
    private final JobRepository jobRepository;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenRouterChatbotService(
            @Value("${openrouter.api.key}") String apiKey,
            @Value("${openrouter.api.url}") String apiUrl,
            @Value("${openrouter.model}") String model,
            ChatHistoryRepository chatHistoryRepository,
            JobRepository jobRepository) {
        this.restTemplate = new RestTemplate();
        this.chatHistoryRepository = chatHistoryRepository;
        this.jobRepository = jobRepository;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.objectMapper = new ObjectMapper();
    }

    private String formatSalary(double salary) {
        if (salary >= 1000000) {
            return String.format("%.1f triệu", salary / 1000000);
        } else {
            return String.format("%.0f nghìn", salary / 1000);
        }
    }

    private String getActiveJobsInfo() {
        List<Job> activeJobs = jobRepository.findByActiveTrue();
        if (activeJobs.isEmpty()) {
            return "Hiện tại không có tin tuyển dụng nào đang active.";
        }

        return activeJobs.stream()
            .map(job -> String.format(
                "Vị trí: %s\n" +
                "Công ty: %s\n" +
                "Địa điểm: %s\n" +
                "Mức lương: %s\n" +
                "Kỹ năng: %s\n" +
                "-------------------",
                job.getName(),
                job.getCompany(),
                job.getLocation(),
                formatSalary(job.getSalary()),
                job.getSkills()
            ))
            .collect(Collectors.joining("\n"));
    }

    public ChatResponse processMessage(ChatRequest request) {
        try {
            // Lấy thông tin việc làm active
            String activeJobsInfo = getActiveJobsInfo();

            // Chuẩn bị headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", "http://localhost:8080/");
            headers.set("X-Title", "JobHunter Chatbot");

            // Chuẩn bị messages
            List<Map<String, String>> messages = new ArrayList<>();
            
            // Thêm system message với thông tin việc làm
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Bạn là một chuyên gia tư vấn việc làm, đặc biệt là trong lĩnh vực công nghệ thông tin. " +
                    "Hãy trả lời các câu hỏi liên quan đến việc làm, nghề nghiệp, kỹ năng và định hướng nghề nghiệp. " +
                    "Hãy trả lời ngắn gọn, súc tích và hữu ích.\n\n" +
                    "Dưới đây là danh sách các tin tuyển dụng đang active:\n" +
                    activeJobsInfo + "\n\n" +
                    "Khi người dùng hỏi về việc làm, hãy dựa vào thông tin trên để tư vấn phù hợp. " +
                    "Nếu không có thông tin phù hợp, hãy tư vấn chung về kỹ năng và định hướng nghề nghiệp.");
            messages.add(systemMessage);

            // Thêm user message
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);

            // Chuẩn bị request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000);

            // Gọi API
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Xử lý response
            String botResponse;
            if (response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    botResponse = message.get("content");
                } else {
                    botResponse = "Xin lỗi, tôi không hiểu câu hỏi của bạn.";
                }
            } else {
                botResponse = "Xin lỗi, tôi không hiểu câu hỏi của bạn.";
            }

            // Lưu lịch sử chat
            ChatHistory history = new ChatHistory();
            history.setUserId(request.getUserId());
            history.setQuestion(request.getMessage());
            history.setAnswer(botResponse);
            history.setTimestamp(LocalDateTime.now());
            chatHistoryRepository.save(history);

            return new ChatResponse(botResponse, LocalDateTime.now());
        } catch (Exception e) {
            return new ChatResponse("Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.", LocalDateTime.now());
        }
    }

    public List<ChatHistory> getUserChatHistory(String userId) {
        return chatHistoryRepository.findByUserIdOrderByTimestampDesc(userId);
    }
} 