package com.kleadingsolutions.expenseshare.controller;

import com.kleadingsolutions.expenseshare.dto.CreateExpenseRequest;
import com.kleadingsolutions.expenseshare.model.Expense;
import com.kleadingsolutions.expenseshare.service.AuthService;
import com.kleadingsolutions.expenseshare.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody CreateExpenseRequest request) {
        UUID actor = authService.getCurrentUserId();
        Expense created = expenseService.createExpense(request, actor);
        return ResponseEntity.ok(created);
    }
}