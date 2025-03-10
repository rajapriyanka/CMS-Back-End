package com.cms.repository;

import com.cms.entities.FacultyCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacultyCourseRepository extends JpaRepository<FacultyCourse, Long> {

    Optional<FacultyCourse> findByFacultyIdAndCourseIdAndBatchId(Long facultyId, Long courseId, Long batchId);

    List<FacultyCourse> findByFacultyId(Long facultyId);
    
    // Update to return the count of deleted entries
    @Modifying
    @Query("DELETE FROM FacultyCourse fc WHERE fc.faculty.id = :facultyId")
    int deleteByFacultyId(@Param("facultyId") Long facultyId);

	Optional<FacultyCourse> findByFacultyIdAndCourseId(Long facultyId, Long courseId);
}

