package ru.anikeeva.finance.repositories.budget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.anikeeva.finance.entities.budget.Budget;
import ru.anikeeva.finance.entities.enums.EBudgetPeriod;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.user.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    boolean existsByUserAndPeriodAndCategory(User user, EBudgetPeriod period, ETransactionCategory category);

    Page<Budget> findAllByUser(User user, Pageable pageable);

    List<Budget> findAllByUserAndCategory(User user, ETransactionCategory category);
}