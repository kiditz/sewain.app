package app.sewain;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class RentApplicationTests {

	@Test
	void verifiesModuleBoundariesAndWritesDocumentation() {

		var modules = ApplicationModules.of(SewainApplication.class).verify();
		new Documenter(modules)
				.writeDocumentation();
	}


}
