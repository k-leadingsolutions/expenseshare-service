package com.kleadingsolutions.expenseshare.repository;

import com.kleadingsolutions.expenseshare.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    /**
     * Return true if a group with the given id exists and its createdBy equals the provided user id.
     * Spring Data JPA will implement this method automatically.
     */
    boolean existsByIdAndCreatedBy(UUID id, UUID createdBy);
}