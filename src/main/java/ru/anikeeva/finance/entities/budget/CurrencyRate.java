package ru.anikeeva.finance.entities.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import ru.anikeeva.finance.entities.enums.ECurrencySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "currency_rates")
public class CurrencyRate {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "value_in_relation_to_base_currency")
    private BigDecimal valueInRelationToBaseCurrency;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "source")
    private ECurrencySource source;

    @Override
    public String toString() {
        return "CurrencyRate of " + currency + ", updatedAt=" + updatedAt;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((currency == null) ? 0 : currency.hashCode());
        result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CurrencyRate other = (CurrencyRate) obj;
        if (currency == null) {
            if (other.currency != null) return false;
        }
        else if (!currency.equals(other.currency)) return false;
        if (updatedAt == null) {
            if (other.updatedAt != null) return false;
        }
        else if (!updatedAt.equals(other.updatedAt)) return false;
        return source == other.source;
    }
}