package com.cms.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class OtpService {

    // Store OTPs with expiration time (in a real application, this should be in a database or cache)
    private final Map<String, OtpData> otpMap = new HashMap<>();
    
    // OTP expiration time in minutes
    private static final int OTP_EXPIRY_MINUTES = 10;
    
    /**
     * Generate a new OTP for the given email
     */
    public String generateOtp(String email) {
        String otp = generateRandomOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpMap.put(email, new OtpData(otp, expiryTime));
        return otp;
    }
    
    /**
     * Validate the OTP for the given email
     */
    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpMap.get(email);
        if (otpData == null) {
            return false;
        }
        
        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpMap.remove(email);
            return false;
        }
        
        return otpData.otp.equals(otp);
    }
    
    /**
     * Clear the OTP for the given email
     */
    public void clearOtp(String email) {
        otpMap.remove(email);
    }
    
    /**
     * Generate a random 6-digit OTP
     */
    private String generateRandomOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    /**
     * Inner class to store OTP data with expiration time
     */
    private static class OtpData {
        private final String otp;
        private final LocalDateTime expiryTime;
        
        public OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
}

