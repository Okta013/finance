package ru.anikeeva.finance.repositories.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.anikeeva.finance.entities.user.User;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.balance = u.balance + :changes WHERE u.id = :id")
    int updateBalance(@Param("id") UUID id, @Param("changes") BigDecimal changes);
}