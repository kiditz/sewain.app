package app.sewain.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MediaRepository
      extends JpaRepository<Media, UUID>,
		JpaSpecificationExecutor<Media> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select media from Media media where media.id = :id")
    Optional<Media> findByIdForUpdate(@Param("id") UUID id);
}
