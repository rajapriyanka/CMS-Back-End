package com.cms.repository;

import com.cms.entities.Faculty;
import com.cms.entities.TimetableEntry;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findByFacultyId(Long facultyId);
    
    void deleteByFaculty(Faculty faculty);
    
    List<TimetableEntry> findByBatchIdAndAcademicYearAndSemester(Long batchId, String academicYear, String semester);
    
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.day = :day AND ts.periodNumber = :periodNumber AND ts.isBreak = false")
    Optional<TimetableEntry> findByFacultyIdAndDayAndPeriod(@Param("facultyId") Long facultyId, @Param("day") DayOfWeek day, @Param("periodNumber") Integer periodNumber);
    
    @Query("SELECT te FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.batch.id = :batchId AND ts.day = :day AND ts.periodNumber = :periodNumber AND ts.isBreak = false")
    Optional<TimetableEntry> findByBatchIdAndDayAndPeriod(@Param("batchId") Long batchId, @Param("day") DayOfWeek day, @Param("periodNumber") Integer periodNumber);
    
    @Query("SELECT COUNT(te) FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.periodNumber = 1 AND ts.isBreak = false")
    Long countFirstPeriodsByFacultyId(@Param("facultyId") Long facultyId);
    
    @Query("SELECT COUNT(te) FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.periodNumber = 2 AND ts.isBreak = false")
    Long countSecondPeriodsByFacultyId(@Param("facultyId") Long facultyId);
    
    @Query("SELECT COUNT(te) FROM TimetableEntry te JOIN te.timeSlot ts WHERE te.faculty.id = :facultyId AND ts.periodNumber = 8 AND ts.isBreak = false")
    Long countLastPeriodsByFacultyId(@Param("facultyId") Long facultyId);
    
  
    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry te WHERE te.faculty.id = :facultyId")
    int deleteByFacultyId(@Param("facultyId") Long facultyId);
    
    // Delete timetable entries based on course ID
    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry te WHERE te.course.id = :courseId")
    int deleteByCourseId(@Param("courseId") Long courseId);
    
    // Delete timetable entries based on batch ID
    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry te WHERE te.batch.id = :batchId")
    int deleteByBatchId(@Param("batchId") Long batchId);

    // Delete timetable entries based on faculty and course
    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry te WHERE te.faculty.id = :facultyId AND te.course.id = :courseId AND te.batch.id = :batchId")
    int deleteByFacultyIdAndCourseIdAndBatchId(@Param("facultyId") Long facultyId, @Param("courseId") Long courseId, @Param("batchId") Long batchId);
}
