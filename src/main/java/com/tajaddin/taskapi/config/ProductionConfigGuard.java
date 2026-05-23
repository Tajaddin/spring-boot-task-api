package com.tajaddin.taskapi.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to finish startup when running under the "prod" or "production"
 * Spring profile if the application is still using either insecure dev
 * default:
 * <ul>
 *   <li>{@code spring.datasource.password} == "taskapi" (the docker-compose
 *       and local dev fallback)</li>
 *   <li>{@code app.jwt.secret} == the committed base64 dev secret</li>
 * </ul>
 * Defaults are intentionally kept in application.yml so {@code mvn
 * spring-boot:run} stays one command for local dev. This guard ensures they
 * cannot silently survive into a deployment.
 */
@Component
public class ProductionConfigGuard {

    private static final String DEV_DB_PASSWORD = "taskapi";
    /** Base64("this-is-a-development-only-secret-change-me"). */
    private static final String DEV_JWT_SECRET =
            "dGhpcy1pcy1hLWRldmVsb3BtZW50LW9ubHktc2VjcmV0LWNoYW5nZS1tZQ==";

    private final Environment environment;
    private final String dbPassword;
    private final String jwtSecret;

    public ProductionConfigGuard(
            Environment environment,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${app.jwt.secret:}") String jwtSecret) {
        this.environment = environment;
        this.dbPassword = dbPassword;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    void verify() {
        boolean isProd = false;
        for (String profile : environment.getActiveProfiles()) {
            String p = profile.toLowerCase();
            if (p.equals("prod") || p.equals("production")) {
                isProd = true;
                break;
            }
        }
        if (!isProd) return;

        StringBuilder problems = new StringBuilder();
        if (DEV_DB_PASSWORD.equals(dbPassword)) {
            problems.append(
                    "DATABASE_PASSWORD must be set to a real value in production; "
                            + "the dev default 'taskapi' is rejected. ");
        }
        if (DEV_JWT_SECRET.equals(jwtSecret)) {
            problems.append(
                    "APP_JWT_SECRET must be set in production; the committed dev "
                            + "default is rejected.");
        }
        if (problems.length() > 0) {
            throw new IllegalStateException(
                    "Production startup refused: " + problems.toString().trim());
        }
    }
}
