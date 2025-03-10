package com.cms.repository;

import com.cms.entities.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    List<Attendance> findByFacultyId(Long facultyId);
    
    List<Attendance> findByStudentId(Long studentId);
    
    List<Attendance> findByCourseId(Long courseId);
    
    List<Attendance> findByBatchName(String batchName);
    
    List<Attendance> findByFacultyIdAndCourseIdAndBatchName(Long facultyId, Long courseId, String batchName);
    
    List<Attendance> findByStudentIdAndCourseId(Long studentId, Long courseId);
    

    
    List<Attendance> findByStudentIdAndCourseIdOrderByDateDesc(Long studentId, Long courseId);
}