package ru.anikeeva.finance.repositories.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.anikeeva.finance.entities.auth.RefreshTokenBlackList;

import java.util.UUID;

@Repository
public interface RefreshTokenBlackListRepository extends JpaRepository<RefreshTokenBlackList, UUID> {
    boolean existsByToken(String token);
}