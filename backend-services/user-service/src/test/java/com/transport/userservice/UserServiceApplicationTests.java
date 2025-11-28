package com.transport.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:testdb",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437",
	"jwt.expiration=86400000"
})
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
