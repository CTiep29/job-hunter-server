package vn.ctiep.jobhunter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.ctiep.jobhunter.domain.response.ResLoginDTO;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    @Value("${ctiep.jwt.base64-secret}")
    private String jwtKey;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // Lấy token từ query parameter
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token == null) {
                logger.warn("WebSocket connection attempt without token");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            try {
                // Verify JWT token using NimbusJwtDecoder
                JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                        .macAlgorithm(JWT_ALGORITHM)
                        .build();

                Jwt jwt = jwtDecoder.decode(token);

                // Get user information from token
                Object userClaim = jwt.getClaims().get("user");
                if (userClaim == null) {
                    logger.warn("WebSocket connection attempt with invalid token claims");
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }

                // Convert user claim to UserInsideToken
                ResLoginDTO.UserInsideToken user = objectMapper.convertValue(userClaim, ResLoginDTO.UserInsideToken.class);

                // Add user info to attributes
                attributes.put("userId", user.getId());
                attributes.put("email", user.getEmail());
                attributes.put("name", user.getName());

                logger.info("WebSocket connection authenticated for user: {}", user.getEmail());
                return true;

            } catch (Exception e) {
                logger.error("WebSocket authentication error: {}", e.getMessage());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("WebSocket handshake error: {}", exception.getMessage());
        }
    }
}