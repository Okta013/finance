package ru.anikeeva.finance.entities.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.anikeeva.finance.entities.enums.EBudgetPeriod;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.user.User;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "budgets")
public class Budget {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "limit_amount", nullable = false)
    private BigDecimal limitAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private EBudgetPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ETransactionCategory category;

    @Override
    public String toString() {
        return "Budget [id=" + id + ", user=" + user + ", limitAmount=" + limitAmount + ", period=" + period + ", category=" +
            category + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((limitAmount == null) ? 0 : limitAmount.hashCode());
        result = prime * result + ((period == null) ? 0 : period.hashCode());
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Budget other = (Budget) obj;
        if (id == null) {
            if (other.id != null) return false;
        }
        else if (!id.equals(other.id)) return false;
        if (limitAmount == null) {
            if (other.limitAmount != null) return false;
        }
        else if (!limitAmount.equals(other.limitAmount)) return false;
        if (period != other.period) return false;
        return category == other.category;
    }
}