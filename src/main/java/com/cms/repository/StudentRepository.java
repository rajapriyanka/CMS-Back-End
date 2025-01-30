package com.cms.repository;

import com.cms.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.user")
    List<Student> findAllWithUsers();

    List<Student> findByNameContainingIgnoreCase(String name);
}

