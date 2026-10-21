package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.entities.RefreshTokenEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	/**
	 * @return the number of rows updated: 1 if the token was active, 0 otherwise
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update RefreshTokenEntity t set t.revokedAt = :revokedAt where t.id = :id and t.revokedAt is null")
	int revokeIfNotRevoked(@Param("id") UUID id, @Param("revokedAt") Instant revokedAt);

}
