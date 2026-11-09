package app.sewain;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@SpringBootTest
class RentApplicationTests {

	@Test
	void contextLoads() {

		var modules = ApplicationModules.of(SewainApplication.class).verify();
		new Documenter(modules)
				.writeDocumentation();
	}


}
