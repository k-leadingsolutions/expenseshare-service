package com.kleadingsolutions.expenseshare.repository;

import com.kleadingsolutions.expenseshare.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, UUID> {
    List<ExpenseSplit> findByExpenseId(UUID expenseId);
}