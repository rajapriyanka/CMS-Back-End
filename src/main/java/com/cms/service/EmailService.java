package com.cms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cms.entities.Leave;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private EmailTokenService emailTokenService;
    
    @Value("${spring.mail.enabled:true}")
    private boolean mailEnabled;
    
    @Value("${spring.mail.username:}")
    private String defaultFromEmail;
    
    @Value("${app.base-url:http://localhost:3000}") // Update frontend URL
    private String baseUrl;

    public boolean sendHtmlLeaveRequestEmail(String to, String from, String subject, String body, 
                                            Long leaveId, Long approverId, String senderName, 
                                            String approverName, String fromDate, String toDate) {
        try {
            if (!mailEnabled) {
                logger.warn("Email sending is disabled.");
                return false;
            }

            // Generate approval and rejection tokens
            String approveToken = emailTokenService.generateToken(leaveId, approverId, Leave.LeaveStatus.APPROVED);
            String rejectToken = emailTokenService.generateToken(leaveId, approverId, Leave.LeaveStatus.REJECTED);
            
            // Frontend URLs for approval/rejection
            String approveUrl = baseUrl + "/email-action?token=" + approveToken + "&action=approve";
            String rejectUrl = baseUrl + "/email-action?token=" + rejectToken + "&action=reject";
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setFrom(defaultFromEmail);
            if (from != null && !from.isEmpty()) {
                helper.setReplyTo(from);
            }
            helper.setSubject(subject);
            
            String htmlBody = "<p>Dear " + approverName + ",</p>"
                    + "<p>A leave request has been submitted by " + senderName + " with the following details:</p>"
                    + "<p><strong>Subject:</strong> " + subject + "</p>"
                    + "<p><strong>Reason:</strong> " + body + "</p>"
                    + "<p><strong>Leave Period:</strong> " + fromDate + " to " + toDate + "</p>"
                    + "<p><a href='" + approveUrl + "' style='padding: 10px; background: green; color: white; text-decoration: none;'>Approve</a>"
                    + " &nbsp; "
                    + "<a href='" + rejectUrl + "' style='padding: 10px; background: red; color: white; text-decoration: none;'>Reject</a></p>"
                    + "<p>Thank you.</p>"
                    +"<p>With Regards,</p>"
                    +"<p>CMS.</p>";
            
            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("Leave approval email sent successfully to: {}", to);
            return true;
        } catch (MailException | MessagingException e) {
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
    
    public boolean sendLeaveStatusUpdateEmail(String to, String from, String subject, String body, 
                                             String approverName, String senderName, 
                                             String fromDate, String toDate) {
        try {
            if (!mailEnabled) {
                logger.warn("Email sending is disabled.");
                return false;
            }
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setFrom(defaultFromEmail);
            if (from != null && !from.isEmpty()) {
                helper.setReplyTo(from);
            }
            helper.setSubject(subject);
            
            String htmlBody = "<p>Dear " + senderName + ",</p>"
                    + "<p>Your leave request for the period <strong>" + fromDate + " to " + toDate + "</strong> has been updated by " + approverName + ":</p>"
                    + "<p><strong>Status:</strong> " + body + "</p>"
                    + "<p>If you have any questions, please contact " + approverName + ".</p>"
                    + "<p>Thank you.</p>"
                    +"<p>With Regards,</p>"
                    +"<p>CMS.</p>";
            
            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("Leave status update email sent successfully to: {}", to);
            return true;
        } catch (MailException | MessagingException e) {
            logger.error("Failed to send leave status update email to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
}
