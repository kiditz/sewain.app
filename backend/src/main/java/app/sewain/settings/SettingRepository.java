package app.sewain.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SettingRepository extends JpaRepository<Setting, UUID> {
	Optional<Setting> findByKey(SettingKey key);

	boolean existsByKey(SettingKey key);
}