package com.tajaddin.taskapi.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to finish startup when an insecure dev default is in use:
 * <ul>
 *   <li>{@code app.jwt.secret} == the committed base64 dev secret, unless the
 *       "demo" or "test" profile is active and no production profile is.
 *       Anyone can read that secret and mint a token for any user id.</li>
 *   <li>{@code spring.datasource.password} == "taskapi" (the local dev
 *       fallback in application.yml) under the "prod" or "production" profile.</li>
 * </ul>
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
        boolean isProd = hasProfile("prod", "production");
        boolean isDevProfile = hasProfile("demo", "test");

        StringBuilder problems = new StringBuilder();
        if (isProd && DEV_DB_PASSWORD.equals(dbPassword)) {
            problems.append(
                    "DATABASE_PASSWORD must be set to a real value in production; "
                            + "the dev default 'taskapi' is rejected. ");
        }
        if (DEV_JWT_SECRET.equals(jwtSecret) && (isProd || !isDevProfile)) {
            problems.append(
                    "APP_JWT_SECRET must be set to a real key; the committed dev secret "
                            + "is only accepted under the demo or test profile.");
        }
        if (problems.length() > 0) {
            throw new IllegalStateException(
                    "Startup refused: " + problems.toString().trim());
        }
    }

    private boolean hasProfile(String... names) {
        for (String profile : environment.getActiveProfiles()) {
            for (String name : names) {
                if (profile.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
