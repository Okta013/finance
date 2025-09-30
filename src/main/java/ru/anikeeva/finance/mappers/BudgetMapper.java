package ru.anikeeva.finance.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.anikeeva.finance.dto.budget.CreateBudgetRequest;
import ru.anikeeva.finance.dto.budget.ReadBudgetResponse;
import ru.anikeeva.finance.dto.budget.UpdateBudgetRequest;
import ru.anikeeva.finance.entities.budget.Budget;

@Mapper(componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BudgetMapper {
    Budget fromCreateBudgetRequest(CreateBudgetRequest budgetRequest);

    ReadBudgetResponse fromBudget(Budget budget);

    void updateBudgetFromUpdateBudgetRequest(UpdateBudgetRequest request, @MappingTarget Budget budget);
}