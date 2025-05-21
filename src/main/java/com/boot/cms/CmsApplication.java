package com.boot.cms;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.Setter;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@MapperScan("com.boot.cms.mapper.**")
public class CmsApplication {
	private static final Logger logger = LoggerFactory.getLogger(CmsApplication.class);

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(CmsApplication.class);

		String errorMessage;

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
				errorMessage = "ERROR: SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD must be set in .env";
				logger.error(errorMessage);
				System.err.println(errorMessage);
			}
		} catch (Exception e) {
			errorMessage = "No .env file found or failed to load: ";
			logger.error(errorMessage, e.getMessage(), e);
			System.out.println(errorMessage + e.getMessage());
			// Check system environment variables
			if (System.getenv("SPRING_DATASOURCE_USERNAME") == null || System.getenv("SPRING_DATASOURCE_PASSWORD") == null) {
				errorMessage = "ERROR: SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD must be set";
				logger.error(errorMessage);
				System.out.println(errorMessage);
			}
		}

		application.run(args);
	}
}