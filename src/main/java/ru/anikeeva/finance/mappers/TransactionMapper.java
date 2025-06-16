package ru.anikeeva.finance.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.anikeeva.finance.dto.budget.TransactionResponse;
import ru.anikeeva.finance.dto.budget.UpdateTransactionRequest;
import ru.anikeeva.finance.entities.budget.Transaction;

@Mapper(componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionMapper {
    TransactionResponse toTransactionResponse(Transaction transaction);

    void updateTransactionFromUpdateTransactionRequest(UpdateTransactionRequest updateTransactionRequest,
                                                       @MappingTarget Transaction transaction);
}