package com.cms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cms.entities.Leave;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailTokenService {
    
    @Value("${app.token.secret:defaultSecretKey}")
    private String tokenSecret;
    
    @Value("${app.token.expiry:24}")
    private int tokenExpiryHours;
    
    private final Map<String, TokenData> tokenCache = new HashMap<>();
    
    public String generateToken(Long leaveId, Long approverId, Leave.LeaveStatus action) {
        try {
            String tokenData = leaveId + ":" + approverId + ":" + action + ":" + 
                               LocalDateTime.now().plusHours(tokenExpiryHours) + ":" + tokenSecret;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenData.getBytes());
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            
            // Store token data in cache
            tokenCache.put(token, new TokenData(leaveId, approverId, action, 
                                               LocalDateTime.now().plusHours(tokenExpiryHours)));
            
            return token;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating token", e);
        }
    }
    
    public TokenData validateToken(String token) {
        TokenData data = tokenCache.get(token);
        
        if (data == null) {
            return null;
        }
        
        if (data.getExpiryTime().isBefore(LocalDateTime.now())) {
            tokenCache.remove(token);
            return null;
        }
        
        return data;
    }
    
    public void invalidateToken(String token) {
        tokenCache.remove(token);
    }
    
    public static class TokenData {
        private final Long leaveId;
        private final Long approverId;
        private final Leave.LeaveStatus action;
        private final LocalDateTime expiryTime;
        
        public TokenData(Long leaveId, Long approverId, Leave.LeaveStatus action, LocalDateTime expiryTime) {
            this.leaveId = leaveId;
            this.approverId = approverId;
            this.action = action;
            this.expiryTime = expiryTime;
        }
        
        public Long getLeaveId() {
            return leaveId;
        }
        
        public Long getApproverId() {
            return approverId;
        }
        
        public Leave.LeaveStatus getAction() {
            return action;
        }
        
        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
    }
}

