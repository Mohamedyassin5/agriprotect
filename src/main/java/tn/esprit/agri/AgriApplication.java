package tn.esprit.agri;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class AgriApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriApplication.class, args);
    }

    @Bean
    public CommandLineRunner databaseRepairRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                System.out.println("=== CRITICAL: DATABASE AUTO-REPAIR STARTING ===");

                // Attempt 1: Cast to CHAR and match '0000...'
                int fixedCreated1 = jdbcTemplate.update(
                        "UPDATE users SET created_at = NOW() WHERE CAST(created_at AS CHAR) = '0000-00-00 00:00:00' OR created_at < '1970-01-01'");

                int fixedUpdated1 = jdbcTemplate.update(
                        "UPDATE users SET updated_at = NOW() WHERE CAST(updated_at AS CHAR) = '0000-00-00 00:00:00' OR updated_at < '1970-01-01'");

                // Attempt 3: Fix invalid/empty roles and statuses
                int fixedRoles = jdbcTemplate.update(
                        "UPDATE users SET role = 'FARMER' WHERE role IS NULL OR role = '' OR role NOT IN ('FARMER', 'ADMIN', 'EXPERT')");
                int fixedStatus = jdbcTemplate.update(
                        "UPDATE users SET status = 'ACTIVE' WHERE status IS NULL OR status = '' OR status NOT IN ('ACTIVE', 'INACTIVE')");

                if (fixedCreated1 > 0 || fixedUpdated1 > 0 || fixedRoles > 0 || fixedStatus > 0) {
                    System.out.println("REPAIR SUCCESS: Fixed " + fixedCreated1 + " created_at, " + fixedUpdated1
                            + " updated_at, " + fixedRoles + " roles, and " + fixedStatus + " statuses.");
                } else {
                    // Attempt 2: Direct match if JDBC allows it
                    jdbcTemplate
                            .execute("UPDATE users SET created_at = NOW() WHERE created_at = '0000-00-00 00:00:00'");
                    jdbcTemplate
                            .execute("UPDATE users SET updated_at = NOW() WHERE updated_at = '0000-00-00 00:00:00'");
                    System.out.println("REPAIR CHECK: Maintenance attempt completed.");
                }

                System.out.println("=== CRITICAL: DATABASE AUTO-REPAIR COMPLETED ===");
            } catch (Exception e) {
                System.err.println("REPAIR WARNING: Could not run auto-repair script: " + e.getMessage());
            }
        };
    }
}
