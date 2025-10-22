package com.kleadingsolutions.expenseshare.repository;

import com.kleadingsolutions.expenseshare.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
}