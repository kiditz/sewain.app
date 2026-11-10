package app.sewain.catalogs;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.data.jpa.repository.Query;

import java.util.Arrays;

/**
 * Parse repository HQL against the real entity model without connecting to a database.
 */
class StorefrontQueryValidationTest {
	@Test
	void paginatedCollectionAndTypeQueriesAreValidForTheEntityModel() throws Exception {
		var registry = new StandardServiceRegistryBuilder()
				.applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
				.applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
				.applySetting("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
				.applySetting("hibernate.hbm2ddl.auto", "none")
				.build();
		try {
			var sources = new MetadataSources(registry);
			var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:app/sewain/**/*.class");
			var readers = new CachingMetadataReaderFactory();
			for (var resource : resources) {
				var metadata = readers.getMetadataReader(resource);
				if (metadata.getAnnotationMetadata().hasAnnotation("jakarta.persistence.Entity")) {
					sources.addAnnotatedClass(Class.forName(metadata.getClassMetadata().getClassName()));
				}
			}
			try (var factory = sources.buildMetadata().buildSessionFactory(); var session = factory.openSession()) {
				var collectionQuery = Arrays.stream(CollectionItemRepository.class.getDeclaredMethods())
						.filter(method -> method.getName().equals("findProducts")).findFirst().orElseThrow().getAnnotation(Query.class);
				session.createQuery(collectionQuery.value(), Product.class);
				session.createQuery(collectionQuery.countQuery(), Long.class);
				var typeQuery = ProductRepository.class.getDeclaredMethod("findTypesByStatus", ProductStatus.class).getAnnotation(Query.class);
				session.createQuery(typeQuery.value(), ProductType.class);
				var cb = session.getCriteriaBuilder();
				var criteria = cb.createQuery(Product.class);
				var root = criteria.from(Product.class);
				criteria.select(root).where(ProductSpecifications.search(java.util.Map.of(
						"id", java.util.UUID.randomUUID() + "," + java.util.UUID.randomUUID(),
						"tags.id", java.util.UUID.randomUUID() + "," + java.util.UUID.randomUUID(),
						"status", "ACTIVE", "available", "true"
				)).toPredicate(root, criteria, cb));
				session.createQuery(criteria);
				var count = cb.createQuery(Long.class);
				var countRoot = count.from(Product.class);
				count.select(cb.count(countRoot)).where(ProductSpecifications.search(java.util.Map.of(
						"tags.id", java.util.UUID.randomUUID().toString()
				)).toPredicate(countRoot, count, cb));
				session.createQuery(count);
			}
		} finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}
}
