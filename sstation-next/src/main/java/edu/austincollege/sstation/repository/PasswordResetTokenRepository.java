package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  /** Looks up a token by its stored hash, eagerly loading the owning user for the reset. */
  @Query("select t from PasswordResetToken t left join fetch t.user where t.tokenHash = :hash")
  Optional<PasswordResetToken> findByTokenHash(@Param("hash") String hash);
}
