package ru.anikeeva.finance.services.budget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.anikeeva.finance.dto.budget.OpenExchangeRatesResponse;
import ru.anikeeva.finance.entities.budget.CurrencyRate;
import ru.anikeeva.finance.entities.enums.ECurrencySource;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.repositories.budget.CurrencyRateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyRateService {
    private final WebClient webClient;
    private final CurrencyRateRepository currencyRateRepository;

    @Value("${currency-rates.url.central-bank}")
    private String centralBankUrl;

    @Value("${currency-rates.url.open-exchange.base}")
    private String openExchangeBaseUrl;

    @Value("${currency-rates.url.open-exchange.additional}")
    private String openExchangeAdditionalUrl;

    @Value("${currency-rates.open-exchange.api-id}")
    private String openExchangeApiId;

    public Mono<Void> updateCurrencyRatesWithFallback() {
        return updateCurrencyRatesFromCentralBank()
            .doOnError(e -> log.warn("Не удалось обновить курсы с ЦБ РФ, пытаемся использовать " +
                "OpenExchangeRates", e))
            .onErrorResume(e -> updateCurrencyRatesFromOpenExchange());
    }

    public Mono<Void> updateCurrencyRatesFromCentralBank() {
        log.info("Запуск обновления курсов валют из ЦБ РФ");
        return webClient.get()
            .uri(centralBankUrl)
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(html -> {
                try {
                    Document doc = Jsoup.parse(html);
                    Element table = doc.select("table.data").first();
                    List<CurrencyRate> currencyRates = new ArrayList<>();
                    if (table == null) {
                        log.warn("Таблица с курсами не найдена на сайте ЦБ РФ");
                        return Mono.empty();
                    }
                    for (Element row : table.select("tr")) {
                        Elements cols = row.select("td");
                        if (!cols.isEmpty()) {
                            String charCode = cols.get(1).text();
                            int units = Integer.parseInt(cols.get(2).text());
                            String currencyName = cols.get(3).text();
                            BigDecimal rateValue = new BigDecimal(cols.get(4).text().replace(",", "."));
                            Currency currency = Currency.getInstance(charCode);
                            BigDecimal valueInBase = rateValue
                                .divide(new BigDecimal(units), 6, RoundingMode.HALF_UP);
                            CurrencyRate currencyRate = CurrencyRate.builder()
                                .currency(currency)
                                .name(currencyName)
                                .valueInRelationToBaseCurrency(valueInBase)
                                .source(ECurrencySource.CENTRAL_BANK)
                                .build();
                            currencyRates.add(currencyRate);
                        }
                    }
                    log.info("Завершено обновление курсов валют с сайта ЦБ РФ. Обновлено {} записей",
                        currencyRates.size());
                    return Mono.fromCallable(() -> currencyRateRepository.saveAll(currencyRates)).then();
                } catch (Exception e) {
                    log.error("Ошибка парсинга курсов валют с сайта ЦБ РФ", e);
                    return Mono.error(e);
                }
            }).doOnError(e -> log.error("Ошибка при обновлении курсов валют с сайта ЦБ РФ"));
    }

    public Mono<Void> updateCurrencyRatesFromOpenExchange() {
        log.info("Запуск обновления курсов валют из OpenExchangeRates");
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(openExchangeBaseUrl)
                .path(openExchangeAdditionalUrl)
                .queryParam("app_id", openExchangeApiId)
                .build())
            .retrieve()
            .bodyToMono(OpenExchangeRatesResponse.class)
            .flatMap(response -> {
                try {
                    BigDecimal rubToUsd = response.rates().get("RUB");
                    if (rubToUsd == null) {
                        log.error("В OpenExchangeRates не найден курс рубля к доллару США");
                        return Mono.error(new RuntimeException("Курс RUB не найден в ответе OpenExchangeRates"));
                    }
                    BigDecimal usdToRub = BigDecimal.ONE.divide(rubToUsd, 6, RoundingMode.HALF_UP);
                    List<CurrencyRate> currencyRates = response.rates().entrySet().stream()
                        .map(entry -> {
                            Currency currency = Currency.getInstance(entry.getKey());
                            BigDecimal rateToRub = entry.getValue().multiply(usdToRub);
                            String nameInRussian = currency.getDisplayName(Locale.of("ru"));
                            return CurrencyRate.builder()
                                .currency(currency)
                                .name(nameInRussian)
                                .valueInRelationToBaseCurrency(rateToRub)
                                .source(ECurrencySource.OPEN_EXCHANGE)
                                .build();
                        })
                        .toList();
                    log.info("Завершено обновление курсов валют с OpenExchangeRates. Обновлено {} записей",
                        currencyRates.size());
                    return Mono.fromCallable(() -> currencyRateRepository.saveAll(currencyRates)).then();
                } catch (Exception e) {
                    log.error("Ошибка парсинга курсов валют с OpenExchangeRates", e);
                    return Mono.error(e);
                }
            }).doOnError(e -> log.error("Ошибка при обновлении курсов валют с OpenExchangeRates", e));
    }

    public CurrencyRate getCurrencyRateByCurrency(Currency currency) {
        Optional<CurrencyRate> currencyRateByCentralBank = currencyRateRepository
            .findTopByCurrencyAndSourceOrderByUpdatedAtDesc(currency, ECurrencySource.CENTRAL_BANK);
        if (currencyRateByCentralBank.isPresent()) return currencyRateByCentralBank.get();
        else {
            Optional<CurrencyRate> currencyRateByOpenExchange = currencyRateRepository
                .findTopByCurrencyAndSourceOrderByUpdatedAtDesc(currency, ECurrencySource.OPEN_EXCHANGE);
            if (currencyRateByOpenExchange.isPresent()) return currencyRateByOpenExchange.get();
            else {
                throw new EntityNotFoundException("Курс валюты не найден");
            }
        }
    }
}