package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Ответ с курсами валют от сервиса OpenExchangeRates")
public record OpenExchangeRatesResponse(
    String base,
    Map<String, BigDecimal> rates
)
{}