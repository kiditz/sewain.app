package app.sewain.merchants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
class MerchantSchemaService {
	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;
	private final MerchantRepository merchantRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void createTenantSchema(String schemaName) {

		log.info("START create tenant schema [{}]", schemaName);

		Flyway flyway = Flyway.configure()
				.dataSource(dataSource)
				.schemas(schemaName)
				.createSchemas(true)
				.locations("classpath:db/tenant")
				.load();

		log.info("Flyway configured for [{}]", schemaName);

		flyway.migrate();

		log.info("DONE migrate tenant schema [{}]", schemaName);

		Boolean exists = jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)",
				Boolean.class,
				schemaName
		);

		log.info("Schema [{}] exists: {}", schemaName, exists);
	}

	void migrateAllTenantSchemas() {
		List<String> schemas = merchantRepository.findAll().stream().map(Merchant::getSchema).toList();
		log.info("START migrating [{}] tenant schemas", schemas.size());
		schemas.forEach(schema -> {
			try {
				createTenantSchema(schema);
			} catch (Exception e) {
				log.error(
						"FAILED migrating tenant schema [{}]",
						schema,
						e
				);
				throw e;
			}

		});
	}
}

