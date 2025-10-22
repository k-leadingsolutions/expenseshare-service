package com.kleadingsolutions.expenseshare.service;

import com.kleadingsolutions.expenseshare.dto.CreateExpenseRequest;
import com.kleadingsolutions.expenseshare.model.Expense;

import java.util.UUID;

public interface ExpenseService {
    /**
     * Create an expense with splits. This method is transactional: it will create expense, splits,
     * ledger entries and update balances atomically.
     *
     * @param request create expense request (groupId, description, amount, currency, splits)
     * @param actorId the currently authenticated user's id (the payer)
     * @return created Expense
     */
    Expense createExpense(CreateExpenseRequest request, UUID actorId);
}