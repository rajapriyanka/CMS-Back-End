package com.cms.service;

import com.cms.dto.TimetableEntryDTO;
import com.cms.dto.TimetableGenerationDTO;
import com.cms.entities.*;
import com.cms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimetableService {

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private FacultyCourseRepository facultyCourseRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private TimeSlotService timeSlotService;

    // Maximum number of labs allowed per batch per day
    private static final int MAX_LABS_PER_DAY = 2;

    @Transactional
    public List<TimetableEntryDTO> generateTimetable(TimetableGenerationDTO dto) {
        // Initialize time slots if not already done
        timeSlotService.initializeTimeSlots();

        // Get faculty
        Faculty faculty = facultyRepository.findById(dto.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        // Get faculty courses
        List<FacultyCourse> facultyCourses = facultyCourseRepository.findByFacultyId(dto.getFacultyId());
        if (facultyCourses.isEmpty()) {
            throw new RuntimeException("No courses assigned to faculty");
        }

        // Clear existing timetable entries for this faculty
        List<TimetableEntry> existingEntries = timetableEntryRepository.findByFacultyId(dto.getFacultyId());
        timetableEntryRepository.deleteAll(existingEntries);

        // Generate new timetable
        List<TimetableEntry> generatedEntries = generateTimetableEntries(faculty, facultyCourses, dto.getAcademicYear(), dto.getSemester());
        timetableEntryRepository.saveAll(generatedEntries);

        // Convert to DTOs
        return convertToDTO(generatedEntries);
    }

    private List<TimetableEntry> generateTimetableEntries(Faculty faculty, List<FacultyCourse> facultyCourses, String academicYear, String semester) {
        List<TimetableEntry> entries = new ArrayList<>();
        List<TimeSlot> allTimeSlots = timeSlotService.getAllNonBreakTimeSlots();
        
        // Group courses by type
        Map<Course.CourseType, List<FacultyCourse>> coursesByType = facultyCourses.stream()
                .collect(Collectors.groupingBy(fc -> fc.getCourse().getType()));
        
        List<FacultyCourse> theoryCourses = coursesByType.getOrDefault(Course.CourseType.ACADEMIC, new ArrayList<>());
        List<FacultyCourse> labCourses = coursesByType.getOrDefault(Course.CourseType.LAB, new ArrayList<>());
        List<FacultyCourse> nonAcademicCourses = coursesByType.getOrDefault(Course.CourseType.NON_ACADEMIC, new ArrayList<>());
        
        // Shuffle to randomize allocation
        Collections.shuffle(theoryCourses);
        Collections.shuffle(labCourses);
        Collections.shuffle(nonAcademicCourses);
        
        // Track lab allocations per batch per day
        Map<Long, Map<DayOfWeek, Integer>> batchLabCountByDay = new HashMap<>();
        
        // Allocate lab courses first (they have more constraints)
        for (FacultyCourse labCourse : labCourses) {
            allocateLabCourse(faculty, labCourse, allTimeSlots, entries, academicYear, semester, batchLabCountByDay);
        }
        
        // Allocate non-academic courses next
        for (FacultyCourse nonAcademicCourse : nonAcademicCourses) {
            allocateNonAcademicCourse(faculty, nonAcademicCourse, allTimeSlots, entries, academicYear, semester);
        }
        
        // Allocate theory courses last
        for (FacultyCourse theoryCourse : theoryCourses) {
            allocateTheoryCourse(faculty, theoryCourse, allTimeSlots, entries, academicYear, semester);
        }
        
        return entries;
    }
    
    private void allocateLabCourse(Faculty faculty, FacultyCourse labCourse, List<TimeSlot> allTimeSlots, 
                                  List<TimetableEntry> entries, String academicYear, String semester,
                                  Map<Long, Map<DayOfWeek, Integer>> batchLabCountByDay) {
        // Labs need consecutive periods and should not be in first period
        int totalPeriodsNeeded = labCourse.getCourse().getContactPeriods();
        
        // If lab has 8 periods, divide by 2 to get 4 continuous periods on different days
        int periodsPerDay = totalPeriodsNeeded > 4 ? totalPeriodsNeeded / 2 : totalPeriodsNeeded;
        int daysNeeded = totalPeriodsNeeded / periodsPerDay;
        
        // Keep track of days already allocated for this lab
        Set<DayOfWeek> allocatedDays = new HashSet<>();
        Long batchId = labCourse.getBatch().getId();
        
        // Initialize lab count for this batch if not already done
        if (!batchLabCountByDay.containsKey(batchId)) {
            batchLabCountByDay.put(batchId, new HashMap<>());
            
            // Pre-populate with existing lab counts from current entries
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day != DayOfWeek.SUNDAY) {
                    int existingCount = countLabsForBatchOnDay(labCourse.getBatch(), day, entries);
                    if (existingCount > 0) {
                        batchLabCountByDay.get(batchId).put(day, existingCount);
                    }
                }
            }
        }
        
        // Allocate lab periods across different days
        for (int day = 0; day < daysNeeded; day++) {
            // Find suitable consecutive slots (not in first period)
            for (DayOfWeek dayOfWeek : Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                                                  DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
                
                // Skip days already allocated for this lab
                if (allocatedDays.contains(dayOfWeek)) {
                    continue;
                }

                // Check if batch already has MAX_LABS_PER_DAY labs on this day
                int existingLabsOnDay = batchLabCountByDay.get(batchId).getOrDefault(dayOfWeek, 0);
                if (existingLabsOnDay >= MAX_LABS_PER_DAY) {
                    continue; // Skip this day if batch already has maximum labs
                }
                
                List<TimeSlot> daySlots = allTimeSlots.stream()
                    .filter(ts -> ts.getDay() == dayOfWeek && ts.getPeriodNumber() > 1) // Not in first period
                    .sorted(Comparator.comparing(TimeSlot::getPeriodNumber))
                    .collect(Collectors.toList());
                
                // Find consecutive slots
                for (int i = 0; i <= daySlots.size() - periodsPerDay; i++) {
                    List<TimeSlot> consecutiveSlots = daySlots.subList(i, i + periodsPerDay);
                    
                    // Check if these slots are consecutive
                    boolean areConsecutive = true;
                    for (int j = 0; j < consecutiveSlots.size() - 1; j++) {
                        if (consecutiveSlots.get(j + 1).getPeriodNumber() != consecutiveSlots.get(j).getPeriodNumber() + 1) {
                            areConsecutive = false;
                            break;
                        }
                    }
                    
                    if (areConsecutive && canAllocateSlots(faculty, labCourse.getBatch(), consecutiveSlots, entries)) {
                        // Allocate all consecutive slots for this lab
                        for (TimeSlot slot : consecutiveSlots) {
                            TimetableEntry entry = new TimetableEntry(
                                faculty, 
                                labCourse.getCourse(), 
                                labCourse.getBatch(), 
                                slot, 
                                academicYear, 
                                semester
                            );
                            entries.add(entry);
                        }
                        
                        // Mark this day as allocated
                        allocatedDays.add(dayOfWeek);
                        
                        // Increment lab count for this batch on this day
                        batchLabCountByDay.get(batchId).put(dayOfWeek, existingLabsOnDay + 1);
                        
                        break; // Successfully allocated for this day
                    }
                }
                
                // If we've allocated for this day, break the day loop
                if (allocatedDays.size() > day) {
                    break;
                }
            }
        }
    }

    private int countLabsForBatchOnDay(Batch batch, DayOfWeek day, List<TimetableEntry> entries) {
        // Count labs already allocated to this batch on this day
        return (int) entries.stream()
            .filter(e -> e.getBatch().getId().equals(batch.getId()) && 
                    e.getTimeSlot().getDay() == day && 
                    e.getCourse().getType() == Course.CourseType.LAB)
            .map(e -> e.getCourse().getId()) // Group by course ID
            .distinct() // Count each lab course only once
            .count();
    }
    
    private void allocateNonAcademicCourse(Faculty faculty, FacultyCourse nonAcademicCourse, List<TimeSlot> allTimeSlots, 
                                         List<TimetableEntry> entries, String academicYear, String semester) {
        int periodsToAllocate = nonAcademicCourse.getCourse().getContactPeriods();
        int periodsAllocated = 0;
        
        // Keep track of days already allocated for this non-academic course
        Set<DayOfWeek> allocatedDays = new HashSet<>();
        
        // Distribute non-academic periods throughout the week (one per day)
        while (periodsAllocated < periodsToAllocate) {
            // Get available days that haven't been allocated yet for this course
            List<DayOfWeek> availableDays = Arrays.asList(DayOfWeek.values()).stream()
                .filter(day -> day != DayOfWeek.SUNDAY && !allocatedDays.contains(day))
                .collect(Collectors.toList());
            
            if (availableDays.isEmpty()) {
                // If we've used all days, we might need to allocate more than one period per day
                // This is a fallback if we can't satisfy the constraint
                availableDays = Arrays.asList(DayOfWeek.values()).stream()
                    .filter(day -> day != DayOfWeek.SUNDAY)
                    .collect(Collectors.toList());
            }
            
            Collections.shuffle(availableDays);
            
            boolean allocated = false;
            for (DayOfWeek day : availableDays) {
                // Get slots for this day (excluding first and last periods)
                List<TimeSlot> daySlots = allTimeSlots.stream()
                    .filter(ts -> ts.getDay() == day && 
                           ts.getPeriodNumber() > 1 && // Not first period
                           ts.getPeriodNumber() < getLastPeriodNumber(allTimeSlots, day)) // Not last period
                    .collect(Collectors.toList());
                
                Collections.shuffle(daySlots);
                
                for (TimeSlot slot : daySlots) {
                    if (canAllocateSlot(faculty, nonAcademicCourse.getBatch(), slot, entries)) {
                        TimetableEntry entry = new TimetableEntry(
                            faculty, 
                            nonAcademicCourse.getCourse(), 
                            nonAcademicCourse.getBatch(), 
                            slot, 
                            academicYear, 
                            semester
                        );
                        entries.add(entry);
                        periodsAllocated++;
                        allocated = true;
                        allocatedDays.add(day);
                        break;
                    }
                }
                
                if (allocated) {
                    break;
                }
            }
            
            if (!allocated) {
                // If we can't allocate more periods, break to avoid infinite loop
                break;
            }
            
            if (periodsAllocated >= periodsToAllocate) {
                break;
            }
        }
    }
    
    private int getLastPeriodNumber(List<TimeSlot> allTimeSlots, DayOfWeek day) {
        return allTimeSlots.stream()
            .filter(ts -> ts.getDay() == day)
            .mapToInt(TimeSlot::getPeriodNumber)
            .max()
            .orElse(8); // Default to 8 if no periods found
    }
    
    private void allocateTheoryCourse(Faculty faculty, FacultyCourse theoryCourse, List<TimeSlot> allTimeSlots, 
                                     List<TimetableEntry> entries, String academicYear, String semester) {
        int periodsToAllocate = theoryCourse.getCourse().getContactPeriods();
        int periodsAllocated = 0;
        
        // Keep track of allocated slots for this course to avoid continuous scheduling
        Map<DayOfWeek, Set<Integer>> allocatedPeriodsByDay = new HashMap<>();
        
        // Distribute theory periods throughout the week
        while (periodsAllocated < periodsToAllocate) {
            // Shuffle time slots to randomize allocation
            List<TimeSlot> availableSlots = new ArrayList<>(allTimeSlots);
            Collections.shuffle(availableSlots);
            
            boolean allocated = false;
            for (TimeSlot slot : availableSlots) {
                // Check if this slot would create continuous theory classes
                if (isTheoryClassContinuous(slot, allocatedPeriodsByDay, theoryCourse.getCourse().getId(), entries)) {
                    continue; // Skip this slot to avoid continuous theory classes
                }
            
                if (canAllocateSlot(faculty, theoryCourse.getBatch(), slot, entries)) {
                    TimetableEntry entry = new TimetableEntry(
                        faculty, 
                        theoryCourse.getCourse(), 
                        theoryCourse.getBatch(), 
                        slot, 
                        academicYear, 
                        semester
                    );
                    entries.add(entry);
                
                    // Track this allocation to avoid continuous classes
                    allocatedPeriodsByDay
                        .computeIfAbsent(slot.getDay(), k -> new HashSet<>())
                        .add(slot.getPeriodNumber());
                
                    periodsAllocated++;
                    allocated = true;
                    break;
                }
            }
            
            if (!allocated) {
                // If we can't allocate more periods, break to avoid infinite loop
                break;
            }
            
            if (periodsAllocated >= periodsToAllocate) {
                break;
            }
        }
    }

    // Add this new helper method to check if a theory class would be continuous
    private boolean isTheoryClassContinuous(TimeSlot slot, Map<DayOfWeek, Set<Integer>> allocatedPeriodsByDay, 
                                          Long courseId, List<TimetableEntry> entries) {
        DayOfWeek day = slot.getDay();
        int period = slot.getPeriodNumber();
    
        // Check if the previous or next period is already allocated for this course on this day
        Set<Integer> periodsForDay = allocatedPeriodsByDay.getOrDefault(day, Collections.emptySet());
        if (periodsForDay.contains(period - 1) || periodsForDay.contains(period + 1)) {
            return true; // Would create continuous classes
        }
    
        // Also check existing entries in the current generation process
        boolean hasPreviousPeriod = entries.stream()
            .anyMatch(e -> e.getCourse().getId().equals(courseId) && 
                     e.getTimeSlot().getDay() == day && 
                     e.getTimeSlot().getPeriodNumber() == period - 1);
                 
        boolean hasNextPeriod = entries.stream()
            .anyMatch(e -> e.getCourse().getId().equals(courseId) && 
                     e.getTimeSlot().getDay() == day && 
                     e.getTimeSlot().getPeriodNumber() == period + 1);
    
        return hasPreviousPeriod || hasNextPeriod;
    }
    
    private boolean canAllocateSlot(Faculty faculty, Batch batch, TimeSlot slot, List<TimetableEntry> entries) {
        // Check if faculty is already allocated in this time slot
        boolean facultyBusy = entries.stream()
            .anyMatch(e -> e.getFaculty().getId().equals(faculty.getId()) && 
                     e.getTimeSlot().getDay() == slot.getDay() && 
                     e.getTimeSlot().getPeriodNumber() == slot.getPeriodNumber());
        
        if (facultyBusy) {
            return false;
        }
        
        // Check if batch is already allocated in this time slot
        boolean batchBusy = entries.stream()
            .anyMatch(e -> e.getBatch().getId().equals(batch.getId()) && 
                     e.getTimeSlot().getDay() == slot.getDay() && 
                     e.getTimeSlot().getPeriodNumber() == slot.getPeriodNumber());
        
        if (batchBusy) {
            return false;
        }
        
        // Check existing timetable entries in database
        Optional<TimetableEntry> facultyEntry = timetableEntryRepository.findByFacultyIdAndDayAndPeriod(
            faculty.getId(), slot.getDay(), slot.getPeriodNumber());
        
        if (facultyEntry.isPresent()) {
            return false;
        }
        
        Optional<TimetableEntry> batchEntry = timetableEntryRepository.findByBatchIdAndDayAndPeriod(
            batch.getId(), slot.getDay(), slot.getPeriodNumber());
        
        return !batchEntry.isPresent();
    }
    
    private boolean canAllocateSlots(Faculty faculty, Batch batch, List<TimeSlot> slots, List<TimetableEntry> entries) {
        for (TimeSlot slot : slots) {
            if (!canAllocateSlot(faculty, batch, slot, entries)) {
                return false;
            }
        }
        return true;
    }

    public List<TimetableEntryDTO> getFacultyTimetable(Long facultyId) {
        List<TimetableEntry> entries = timetableEntryRepository.findByFacultyId(facultyId);
        return convertToDTO(entries);
    }

    public List<TimetableEntryDTO> getBatchTimetable(Long batchId, String academicYear, String semester) {
        List<TimetableEntry> entries = timetableEntryRepository.findByBatchIdAndAcademicYearAndSemester(batchId, academicYear, semester);
        return convertToDTO(entries);
    }

    private List<TimetableEntryDTO> convertToDTO(List<TimetableEntry> entries) {
        List<TimetableEntryDTO> dtos = new ArrayList<>();
        
        for (TimetableEntry entry : entries) {
            TimetableEntryDTO dto = new TimetableEntryDTO();
            dto.setId(entry.getId());
            dto.setFacultyName(entry.getFaculty().getName());
            dto.setCourseName(entry.getCourse().getTitle());
            dto.setCourseCode(entry.getCourse().getCode());
            dto.setBatchName(entry.getBatch().getBatchName());
            dto.setDepartment(entry.getBatch().getDepartment());
            dto.setSection(entry.getBatch().getSection());
            dto.setDay(entry.getTimeSlot().getDay());
            dto.setPeriodNumber(entry.getTimeSlot().getPeriodNumber());
            dto.setStartTime(entry.getTimeSlot().getStartTime());
            dto.setEndTime(entry.getTimeSlot().getEndTime());
            dto.setCourseType(entry.getCourse().getType().toString());
            dtos.add(dto);
        }
        
        return dtos;
    }
}

