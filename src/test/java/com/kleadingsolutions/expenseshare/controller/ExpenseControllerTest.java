package com.kleadingsolutions.expenseshare.controller;

import com.kleadingsolutions.expenseshare.dto.CreateExpenseRequest;
import com.kleadingsolutions.expenseshare.model.Expense;
import com.kleadingsolutions.expenseshare.service.AuthService;
import com.kleadingsolutions.expenseshare.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock
    private ExpenseService expenseService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ExpenseController expenseController;

    private UUID actor;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
    }

    @Test
    void createExpense_shouldReturnCreatedExpense() {

        CreateExpenseRequest request = org.mockito.Mockito.mock(CreateExpenseRequest.class);
        Expense created = org.mockito.Mockito.mock(Expense.class);

        when(authService.getCurrentUserId()).thenReturn(actor);
        when(expenseService.createExpense(any(CreateExpenseRequest.class), eq(actor))).thenReturn(created);

        ResponseEntity<Expense> resp = expenseController.createExpense(request);

        // Assert
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCodeValue());
        assertSame(created, resp.getBody());
    }
}