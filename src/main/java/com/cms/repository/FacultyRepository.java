package com.cms.repository;

import com.cms.entities.Faculty;
import com.cms.entities.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    List<Faculty> findByNameContainingIgnoreCase(String name);

	Object findByUser_Email(String email);

	Optional<User> findByUser(User user);
}
