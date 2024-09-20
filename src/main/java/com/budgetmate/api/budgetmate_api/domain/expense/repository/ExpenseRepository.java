package com.budgetmate.api.budgetmate_api.domain.expense.repository;

import com.budgetmate.api.budgetmate_api.domain.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense,Long> {

}
