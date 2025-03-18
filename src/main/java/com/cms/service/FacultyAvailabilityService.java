package com.cms.service;

import com.cms.dto.FacultyAvailabilityDTO;
import com.cms.dto.FacultyFilterDTO;
import com.cms.entities.*;
import com.cms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FacultyAvailabilityService {

    @Autowired
    private FacultyRepository facultyRepository;
    
    @Autowired
    private TimetableEntryRepository timetableEntryRepository;
    
    @Autowired
    private FacultyCourseRepository facultyCourseRepository;
    
    @Autowired
    private SubstituteRequestRepository substituteRequestRepository;
    
    @Autowired
    private TimeSlotRepository timeSlotRepository;

    /**
     * Filter faculty based on availability and batch handling
     */
    public List<FacultyAvailabilityDTO> filterFacultyByAvailabilityAndBatch(FacultyFilterDTO filterDTO) {
        List<Faculty> allFaculty = facultyRepository.findAll();
        
        // Filter out the requesting faculty
        allFaculty = allFaculty.stream()
            .filter(f -> !f.getId().equals(filterDTO.getRequestingFacultyId()))
            .collect(Collectors.toList());
        
        // Get the day of week for the requested date
        DayOfWeek dayOfWeek = filterDTO.getRequestDate().getDayOfWeek();
        
        // Get the time slot for the period
        Optional<TimeSlot> timeSlotOpt = timeSlotRepository.findByDayAndPeriodNumber(dayOfWeek, filterDTO.getPeriodNumber());
        if (!timeSlotOpt.isPresent()) {
            throw new RuntimeException("Time slot not found for the given day and period");
        }
        
        TimeSlot timeSlot = timeSlotOpt.get();
        
        List<FacultyAvailabilityDTO> availableFaculty = new ArrayList<>();
        
        for (Faculty faculty : allFaculty) {
            FacultyAvailabilityDTO availabilityDTO = new FacultyAvailabilityDTO();
            availabilityDTO.setFacultyId(faculty.getId());
            availabilityDTO.setName(faculty.getName());
            availabilityDTO.setDepartment(faculty.getDepartment());
            availabilityDTO.setDesignation(faculty.getDesignation());
            availabilityDTO.setEmail(faculty.getUser().getEmail());
            
            // Check if faculty is available during the requested time slot
            boolean isAvailable = isFacultyAvailable(faculty.getId(), dayOfWeek, filterDTO.getPeriodNumber(), 
                                                   filterDTO.getRequestDate());
            availabilityDTO.setAvailable(isAvailable);
            
            // Check if faculty handles the requested batch
            boolean handlesBatch = false;
            if (filterDTO.getBatchId() != null) {
                handlesBatch = doesFacultyHandleBatch(faculty.getId(), filterDTO.getBatchId());
            }
            availabilityDTO.setHandlesBatch(handlesBatch);
            
            // Add to result list based on filter criteria
            if ((filterDTO.isFilterByAvailability() && !isAvailable) || 
                (filterDTO.isFilterByBatch() && !handlesBatch)) {
                // Skip this faculty as they don't meet the filter criteria
                continue;
            }
            
            availableFaculty.add(availabilityDTO);
        }
        
        return availableFaculty;
    }
    
    /**
     * Check if a faculty is available during a specific time slot
     */
    private boolean isFacultyAvailable(Long facultyId, DayOfWeek day, Integer periodNumber, LocalDate date) {
        // Check if faculty has a class during this time slot
        Optional<TimetableEntry> existingEntry = timetableEntryRepository.findByFacultyIdAndDayAndPeriod(
            facultyId, day, periodNumber);
        
        if (existingEntry.isPresent()) {
            return false; // Faculty has a class during this time
        }
        
        // Check if faculty already has a substitute request for this date
        List<SubstituteRequest> existingRequests = substituteRequestRepository.findBySubstituteIdAndDate(
            facultyId, date);
        
        return existingRequests.isEmpty(); // Faculty is available if no substitute requests exist
    }
    
    /**
     * Check if a faculty handles a specific batch
     */
    private boolean doesFacultyHandleBatch(Long facultyId, Long batchId) {
        List<FacultyCourse> facultyCourses = facultyCourseRepository.findByFacultyId(facultyId);
        
        return facultyCourses.stream()
            .anyMatch(fc -> fc.getBatch().getId().equals(batchId));
    }
}