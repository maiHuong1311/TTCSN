package com.learnflow.backend.auth;

import com.learnflow.backend.auth.domain.RegistrationEmailVerification;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface RegistrationEmailVerificationRepository
    extends JpaRepository<RegistrationEmailVerification, Long>
{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from RegistrationEmailVerification v where v.email = :email")
    Optional<RegistrationEmailVerification> lockByEmail(String email);
}
