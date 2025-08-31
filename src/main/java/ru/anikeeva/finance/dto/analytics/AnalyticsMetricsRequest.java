package ru.anikeeva.finance.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import ru.anikeeva.finance.annotations.DateTimeValid;

@Schema(description = "Запрос на получение сравнения метрик двух периодов")
public record AnalyticsMetricsRequest(
    @Schema(description = "Дата начала первого периода в формате yyyy-MM-ddTHH:mm:ss")
    @NotBlank
    @DateTimeValid
    String startDateForFirstPeriod,

    @Schema(description = "Дата конца первого периода в формате yyyy-MM-ddTHH:mm:ss")
    @NotBlank
    @DateTimeValid
    String endDateForFirstPeriod,

    @Schema(description = "Дата начала второго периода в формате yyyy-MM-ddTHH:mm:ss")
    @NotBlank
    @DateTimeValid
    String startDateForSecondPeriod,

    @Schema(description = "Дата конца второго периода в формате yyyy-MM-ddTHH:mm:ss")
    @NotBlank
    @DateTimeValid
    String endDateForSecondPeriod
)
{}