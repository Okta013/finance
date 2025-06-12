package ru.anikeeva.finance.entities.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "refresh_tokens_black_list")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenBlackList {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Override
    public String toString() {
        return token;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RefreshTokenBlackList other = (RefreshTokenBlackList) obj;
        return (id != null && id.equals(other.id)) ||
            (token != null && token.equals(other.token));
    }
}