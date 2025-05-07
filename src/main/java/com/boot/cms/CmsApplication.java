package com.boot.cms;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@MapperScan("com.boot.cms.mapper.**")
public class CmsApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(CmsApplication.class);

		// Set default profile to 'dev' if not specified
		Map<String, Object> defaultProperties = new HashMap<>();
		defaultProperties.put("spring.profiles.active", "dev");
		application.setDefaultProperties(defaultProperties);

		// Load .env if present
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory("./")
					.filename(".env")
					.ignoreIfMissing()
					.load();
			System.out.println("Loaded .env");
			System.out.println("SPRING_DATASOURCE_URL = " + dotenv.get("SPRING_DATASOURCE_URL"));
			System.out.println("SPRING_DATASOURCE_USERNAME = " + dotenv.get("SPRING_DATASOURCE_USERNAME"));
			System.out.println("SPRING_DATASOURCE_PASSWORD = " + (dotenv.get("SPRING_DATASOURCE_PASSWORD") != null ? "[REDACTED]" : "null"));
			System.out.println("CORS_ALLOWED_ORIGINS = " + dotenv.get("CORS_ALLOWED_ORIGINS"));
			System.out.println("SPRING_PROFILES_ACTIVE = " + dotenv.get("SPRING_PROFILES_ACTIVE"));

			// Add .env to system properties
			dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

			// Validate critical variables
			if (dotenv.get("SPRING_DATASOURCE_USERNAME") == null || dotenv.get("SPRING_DATASOURCE_PASSWORD") == null) {
				System.err.println("ERROR: SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD must be set in .env");
			}
		} catch (Exception e) {
			System.out.println("No .env file found or failed to load: " + e.getMessage());
			// Check system environment variables
			if (System.getenv("SPRING_DATASOURCE_USERNAME") == null || System.getenv("SPRING_DATASOURCE_PASSWORD") == null) {
				System.err.println("ERROR: SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD must be set");
			}
		}

		application.run(args);
	}
}