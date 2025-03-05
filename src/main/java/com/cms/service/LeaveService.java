package com.cms.service;

import com.cms.dto.LeaveActionDTO;
import com.cms.dto.LeaveRequestDTO;
import com.cms.dto.LeaveResponseDTO;
import com.cms.entities.Faculty;
import com.cms.entities.Leave;
import com.cms.entities.User;
import com.cms.repository.FacultyRepository;
import com.cms.repository.LeaveRepository;
import com.cms.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private FacultyRepository facultyRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public LeaveResponseDTO requestLeave(Long facultyId, LeaveRequestDTO leaveRequestDTO) {
        logger.info("Processing leave request for faculty ID: {}", facultyId);
        
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        User approverUser = userRepository.findById(leaveRequestDTO.getApproverId())
                .orElseThrow(() -> new RuntimeException("Approver user not found"));

        Faculty approver = facultyRepository.findByUser(approverUser)
                .orElseThrow(() -> new RuntimeException("Approver faculty not found for user ID: " + leaveRequestDTO.getApproverId()));

        logger.info("Approver Faculty found: {} (Faculty ID: {})", approver.getName(), approver.getId());

        Leave leave = new Leave();
        leave.setFaculty(faculty);
        leave.setApprover(approver);
        leave.setSubject(leaveRequestDTO.getSubject());
        leave.setReason(leaveRequestDTO.getReason());
        leave.setFromDate(leaveRequestDTO.getFromDate());
        leave.setToDate(leaveRequestDTO.getToDate());
        leave.setRequestedAt(LocalDateTime.now());
        leave.setStatus(Leave.LeaveStatus.PENDING);
        
        Leave savedLeave = leaveRepository.save(leave);
        logger.info("Leave request saved with ID: {}", savedLeave.getId());
        
        try {
            String fromDateStr = leave.getFromDate().format(DATE_FORMATTER);
            String toDateStr = leave.getToDate().format(DATE_FORMATTER);
            
            emailService.sendHtmlLeaveRequestEmail(
                approver.getUser().getEmail(),
                faculty.getUser().getEmail(),
                "Leave Request: " + leave.getSubject(),
                leave.getReason(),
                savedLeave.getId(),
                approver.getId(),
                faculty.getName(),
                approver.getName(),
                fromDateStr,
                toDateStr
            );
        } catch (Exception e) {
            logger.error("Error sending email notification for leave request ID {}: {}", savedLeave.getId(), e.getMessage());
        }
        
        return convertToDTO(savedLeave);
    }

    @Transactional
    public LeaveResponseDTO updateLeaveStatus(Long leaveId, Long approverId, LeaveActionDTO leaveActionDTO) {
        logger.info("Updating leave status for leave ID: {} by approver ID: {}", leaveId, approverId);
        
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        
        if (!leave.getApprover().getId().equals(approverId)) {
            throw new RuntimeException("You are not authorized to update this leave request");
        }

        leave.setStatus(leaveActionDTO.getStatus());
        leave.setComments(leaveActionDTO.getComments());
        leave.setRespondedAt(LocalDateTime.now());
        
        Leave updatedLeave = leaveRepository.save(leave);
        logger.info("Leave status updated to {} for leave ID: {}", updatedLeave.getStatus(), updatedLeave.getId());
        
        try {
            String fromDateStr = leave.getFromDate().format(DATE_FORMATTER);
            String toDateStr = leave.getToDate().format(DATE_FORMATTER);
            
            emailService.sendLeaveStatusUpdateEmail(
                leave.getFaculty().getUser().getEmail(),
                leave.getApprover().getUser().getEmail(),
                "Leave Request " + leave.getStatus() + ": " + leave.getSubject(),
                leave.getStatus().toString(),
                leave.getApprover().getName(),
                leave.getFaculty().getName(),
                fromDateStr,
                toDateStr
            );
        } catch (Exception e) {
            logger.error("Error sending email notification for leave status update: {}", e.getMessage());
        }
        
        return convertToDTO(updatedLeave);
    }

    public List<LeaveResponseDTO> getLeaveRequestsByFaculty(Long facultyId) {
        logger.info("Fetching leave requests for faculty ID: {}", facultyId);
        
        // Verify faculty exists
        facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        
        List<Leave> leaves = leaveRepository.findByFacultyId(facultyId);
        logger.info("Found {} leave requests for faculty ID: {}", leaves.size(), facultyId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveResponseDTO> getLeaveRequestsForApprover(Long approverId) {
        logger.info("Fetching leave requests for approver ID: {}", approverId);
        
        // Verify approver exists
        facultyRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver faculty not found"));
        
        List<Leave> leaves = leaveRepository.findByApproverId(approverId);
        logger.info("Found {} leave requests for approver ID: {}", leaves.size(), approverId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveResponseDTO> getPendingLeaveRequestsForApprover(Long approverId) {
        logger.info("Fetching pending leave requests for approver ID: {}", approverId);
        
        // Verify approver exists
        facultyRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Approver faculty not found"));
        
        List<Leave> leaves = leaveRepository.findByApproverIdAndStatus(
                approverId, Leave.LeaveStatus.PENDING);
        logger.info("Found {} pending leave requests for approver ID: {}", leaves.size(), approverId);
        
        return leaves.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private LeaveResponseDTO convertToDTO(Leave leave) {
        LeaveResponseDTO dto = new LeaveResponseDTO();
        dto.setId(leave.getId());
        dto.setFacultyId(leave.getFaculty().getId());
        dto.setFacultyName(leave.getFaculty().getName());
        dto.setFacultyEmail(leave.getFaculty().getUser().getEmail());
        dto.setApproverId(leave.getApprover().getId());
        dto.setApproverName(leave.getApprover().getName());
        dto.setApproverEmail(leave.getApprover().getUser().getEmail());
        dto.setSubject(leave.getSubject());
        dto.setReason(leave.getReason());
        dto.setFromDate(leave.getFromDate());
        dto.setToDate(leave.getToDate());
        dto.setRequestedAt(leave.getRequestedAt());
        dto.setRespondedAt(leave.getRespondedAt());
        dto.setStatus(leave.getStatus());
        dto.setComments(leave.getComments());
        return dto;
    }
}
