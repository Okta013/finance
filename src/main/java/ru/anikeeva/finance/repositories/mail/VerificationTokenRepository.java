package ru.anikeeva.finance.repositories.mail;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.anikeeva.finance.entities.mail.VerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
}